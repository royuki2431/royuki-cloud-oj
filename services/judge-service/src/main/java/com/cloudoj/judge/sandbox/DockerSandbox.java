package com.cloudoj.judge.sandbox;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Docker沙箱基础类
 * 提供Docker容器的创建、执行和清理功能
 * 支持容器池复用，减少容器创建销毁开销
 */
@Slf4j
@Component
public class DockerSandbox {
    
    private final DockerClient dockerClient;
    
    // 资源限制（默认值）
    private static final long DEFAULT_MEMORY_LIMIT = 256 * 1024 * 1024L; // 256MB
    private static final long CPU_COUNT = 1L; // 1核CPU
    private static final int DEFAULT_TIME_LIMIT = 5; // 5秒超时
    
    // 容器池：镜像名 -> 容器ID
    private final Map<String, String> containerPool = new ConcurrentHashMap<>();
    
    // 共享工作目录（用于容器池模式）
    private String sharedWorkDir;
    
    public DockerSandbox() {
        // 初始化Docker客户端
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375") // Windows下使用tcp，Linux下可用unix:///var/run/docker.sock
                .build();
        
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        
        this.dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();
        
        log.info("Docker客户端初始化成功");
    }
    
    /**
     * 初始化共享工作目录
     */
    @PostConstruct
    public void init() {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            sharedWorkDir = tempDir + File.separator + "oj-judge-shared";
            File dir = new File(sharedWorkDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            log.info("共享工作目录初始化成功: {}", sharedWorkDir);
        } catch (Exception e) {
            log.error("共享工作目录初始化失败", e);
        }
    }
    
    /**
     * 获取共享工作目录
     */
    public String getSharedWorkDir() {
        return sharedWorkDir;
    }
    
    /**
     * 应用关闭时清理容器池
     */
    @PreDestroy
    public void destroy() {
        log.info("清理容器池...");
        containerPool.forEach((image, containerId) -> {
            try {
                dockerClient.stopContainerCmd(containerId).withTimeout(1).exec();
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                log.info("清理容器池中的容器: image={}, containerId={}", image, containerId);
            } catch (Exception e) {
                log.error("清理容器失败: {}", containerId, e);
            }
        });
        containerPool.clear();
    }
    
    /**
     * 获取或创建长期运行的容器（容器池模式）
     * 容器挂载共享工作目录，支持多次评测复用
     */
    public String getOrCreatePooledContainer(String image) {
        return containerPool.computeIfAbsent(image, img -> {
            try {
                ensureImageExists(img);
                String containerId = createLongRunningContainer(img);
                log.info("容器池：创建新容器 image={}, containerId={}", img, containerId);
                return containerId;
            } catch (Exception e) {
                log.error("创建容器池容器失败: {}", img, e);
                return null;
            }
        });
    }
    
    /**
     * 创建长期运行的容器（用于容器池）
     * 挂载共享工作目录，支持动态切换子目录
     */
    private String createLongRunningContainer(String image) {
        Volume volume = new Volume("/workspace");
        // 挂载共享工作目录，而不是单次评测的目录
        Bind bind = new Bind(sharedWorkDir, volume);
        
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(bind)
                .withMemory(DEFAULT_MEMORY_LIMIT)
                .withMemorySwap(DEFAULT_MEMORY_LIMIT)
                .withCpuCount(CPU_COUNT)
                .withNetworkMode("none")
                .withAutoRemove(false);
        
        // 创建一个长期运行的容器（使用 tail -f /dev/null 保持运行）
        CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withCmd("tail", "-f", "/dev/null")
                .withWorkingDir("/workspace")
                .withHostConfig(hostConfig)
                .withVolumes(volume)
                .withName("oj-pool-" + image.replace(":", "-").replace("/", "-") + "-" + UUID.randomUUID().toString().substring(0, 4))
                .exec();
        
        // 启动容器
        dockerClient.startContainerCmd(container.getId()).exec();
        log.info("长期运行容器已启动: {}, 挂载目录: {}", container.getId(), sharedWorkDir);
        
        return container.getId();
    }
    
    /**
     * 在已有容器中执行命令（docker exec 模式，更快）
     * @param containerId 容器ID
     * @param command 要执行的命令
     * @param subDir 子目录名称（相对于共享工作目录）
     * @param timeoutSeconds 超时时间
     */
    public DockerExecuteResult executeInContainer(String containerId, String[] command, String subDir, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 工作目录是共享目录下的子目录
            String workingDir = "/workspace/" + subDir;
            
            // 创建 exec 命令
            ExecCreateCmdResponse execCreate = dockerClient.execCreateCmd(containerId)
                    .withCmd(command)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withWorkingDir(workingDir)
                    .exec();
            
            // 执行命令并获取输出
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            
            boolean completed = dockerClient.execStartCmd(execCreate.getId())
                    .exec(new ExecStartResultCallback(stdout, stderr))
                    .awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);
            
            long executeTime = System.currentTimeMillis() - startTime;
            
            // 获取退出码（需要处理 null 情况）
            Integer exitCode = -1;
            if (completed) {
                // 等待一小段时间确保退出码可用
                for (int i = 0; i < 10; i++) {
                    Long exitCodeLong = dockerClient.inspectExecCmd(execCreate.getId()).exec().getExitCodeLong();
                    if (exitCodeLong != null) {
                        exitCode = exitCodeLong.intValue();
                        break;
                    }
                    Thread.sleep(50);
                }
            }
            
            String output = stdout.toString("UTF-8");
            String error = stderr.toString("UTF-8");
            
            // 如果超时，返回超时错误
            if (!completed) {
                log.warn("容器exec执行超时: time={}ms", executeTime);
                return DockerExecuteResult.builder()
                        .success(false)
                        .exitCode(-1)
                        .output(output)
                        .error("Execution timed out")
                        .executeTime(executeTime)
                        .memoryUsed(0)
                        .build();
            }
            
            log.debug("容器exec执行完成: exitCode={}, time={}ms", exitCode, executeTime);
            
            return DockerExecuteResult.builder()
                    .success(exitCode == 0)
                    .exitCode(exitCode)
                    .output(output)
                    .error(error.isEmpty() ? (exitCode != 0 ? output : "") : error)
                    .executeTime(executeTime)
                    .memoryUsed(DEFAULT_MEMORY_LIMIT / 2)
                    .build();
                    
        } catch (Exception e) {
            long executeTime = System.currentTimeMillis() - startTime;
            log.error("容器exec执行失败", e);
            return DockerExecuteResult.builder()
                    .success(false)
                    .exitCode(-1)
                    .output("")
                    .error("执行失败: " + e.getMessage())
                    .executeTime(executeTime)
                    .memoryUsed(0)
                    .build();
        }
    }
    
    /**
     * Exec回调处理器
     */
    private static class ExecStartResultCallback extends com.github.dockerjava.api.async.ResultCallbackTemplate<
            ExecStartResultCallback, com.github.dockerjava.api.model.Frame> {
        
        private final ByteArrayOutputStream stdout;
        private final ByteArrayOutputStream stderr;
        
        public ExecStartResultCallback(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }
        
        @Override
        public void onNext(com.github.dockerjava.api.model.Frame frame) {
            try {
                switch (frame.getStreamType()) {
                    case STDOUT:
                    case RAW:
                        stdout.write(frame.getPayload());
                        break;
                    case STDERR:
                        stderr.write(frame.getPayload());
                        break;
                }
            } catch (Exception e) {
                // 忽略
            }
        }
    }
    
    /**
     * 执行Docker命令
     * 
     * @param image Docker镜像名称
     * @param command 要执行的命令
     * @param workDir 工作目录（宿主机路径）
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     */
    public DockerExecuteResult execute(String image, String[] command, String workDir, int timeoutSeconds) {
        return execute(image, command, workDir, timeoutSeconds, 256);
    }
    
    /**
     * 执行Docker命令（带内存限制）
     * 
     * @param image Docker镜像名称
     * @param command 要执行的命令
     * @param workDir 工作目录（宿主机路径）
     * @param timeoutSeconds 超时时间（秒）
     * @param memoryLimitMB 内存限制（MB）
     * @return 执行结果
     */
    public DockerExecuteResult execute(String image, String[] command, String workDir, int timeoutSeconds, int memoryLimitMB) {
        String containerId = null;
        long startTime = System.currentTimeMillis();
        long memoryLimitBytes = memoryLimitMB * 1024L * 1024L;
        
        try {
            // 确保镜像存在
            ensureImageExists(image);
            
            // 创建容器
            containerId = createContainer(image, command, workDir, memoryLimitBytes);
            log.debug("创建容器成功: {}", containerId);
            
            // 启动容器 - 从这里开始计时（排除镜像检查和容器创建的开销）
            long codeStartTime = System.currentTimeMillis();
            dockerClient.startContainerCmd(containerId).exec();
            log.debug("启动容器成功: {}", containerId);
            
            // 等待容器执行完成（带超时）
            Integer exitCode = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS);
            
            long executeTime = System.currentTimeMillis() - codeStartTime;
            
            // 获取容器输出
            String output = getContainerLogs(containerId);
            
            // 检查是否超时
            if (exitCode == null) {
                log.warn("容器执行超时: {}", containerId);
                return DockerExecuteResult.builder()
                        .success(false)
                        .exitCode(-1)
                        .output("")
                        .error("执行超时")
                        .executeTime(executeTime)
                        .memoryUsed(0)
                        .build();
            }
            
            // 获取容器统计信息（内存使用）
            long memoryUsed = getContainerMemoryUsage(containerId);
            
            log.debug("容器执行完成: containerId={}, exitCode={}, time={}ms", 
                    containerId, exitCode, executeTime);
            
            return DockerExecuteResult.builder()
                    .success(exitCode == 0)
                    .exitCode(exitCode)
                    .output(output)
                    .error(exitCode != 0 ? output : "")
                    .executeTime(executeTime)
                    .memoryUsed(memoryUsed)
                    .build();
            
        } catch (Exception e) {
            long executeTime = System.currentTimeMillis() - startTime;
            log.error("Docker执行失败", e);
            return DockerExecuteResult.builder()
                    .success(false)
                    .exitCode(-1)
                    .output("")
                    .error("系统错误: " + e.getMessage())
                    .executeTime(executeTime)
                    .memoryUsed(0)
                    .build();
        } finally {
            // 清理容器
            if (containerId != null) {
                cleanupContainer(containerId);
            }
        }
    }
    
    /**
     * 创建容器（使用默认内存限制）
     */
    private String createContainer(String image, String[] command, String workDir) {
        return createContainer(image, command, workDir, DEFAULT_MEMORY_LIMIT);
    }
    
    /**
     * 创建容器（带内存限制）
     */
    private String createContainer(String image, String[] command, String workDir, long memoryLimitBytes) {
        // 创建卷挂载
        Volume volume = new Volume("/workspace");
        Bind bind = new Bind(workDir, volume);
        
        // 配置主机资源限制
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(bind)
                .withMemory(memoryLimitBytes)
                .withMemorySwap(memoryLimitBytes) // 禁用swap
                .withCpuCount(CPU_COUNT)
                .withNetworkMode("none") // 禁止网络访问
                .withAutoRemove(false); // 手动清理
        
        // 创建容器
        CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withCmd(command)
                .withWorkingDir("/workspace")
                .withHostConfig(hostConfig)
                .withVolumes(volume)
                .withName("oj-judge-" + UUID.randomUUID().toString().substring(0, 8))
                .exec();
        
        return container.getId();
    }
    
    /**
     * 获取容器日志
     */
    private String getContainerLogs(String containerId) {
        try (ByteArrayOutputStream stdout = new ByteArrayOutputStream();
             ByteArrayOutputStream stderr = new ByteArrayOutputStream()) {
            
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new LogContainerResultCallback(stdout, stderr))
                    .awaitCompletion(5, TimeUnit.SECONDS);
            
            String output = stdout.toString("UTF-8");
            String error = stderr.toString("UTF-8");
            
            if (!error.isEmpty()) {
                return error;
            }
            return output;
            
        } catch (Exception e) {
            log.error("获取容器日志失败", e);
            return "";
        }
    }
    
    /**
     * 获取容器内存使用情况
     */
    private long getContainerMemoryUsage(String containerId) {
        try {
            // 由于Docker API限制，这里返回估计值
            // 实际项目中可以通过stats API获取精确值
            return DEFAULT_MEMORY_LIMIT / 2; // 返回最大限制的一半作为估计值
        } catch (Exception e) {
            log.error("获取容器内存使用失败", e);
            return 0;
        }
    }
    
    /**
     * 确保Docker镜像存在
     */
    private void ensureImageExists(String image) {
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (Exception e) {
            log.warn("镜像不存在，开始拉取: {}", image);
            try {
                dockerClient.pullImageCmd(image)
                        .exec(new PullImageResultCallback())
                        .awaitCompletion(5, TimeUnit.MINUTES);
                log.info("镜像拉取成功: {}", image);
            } catch (Exception ex) {
                log.error("镜像拉取失败: {}", image, ex);
                throw new RuntimeException("Docker镜像不存在且拉取失败: " + image);
            }
        }
    }
    
    /**
     * 清理容器
     */
    private void cleanupContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();
            log.debug("清理容器成功: {}", containerId);
        } catch (Exception e) {
            log.error("清理容器失败: {}", containerId, e);
        }
    }
    
    /**
     * 创建临时工作目录（在共享目录下创建子目录）
     * @return 工作目录的绝对路径
     */
    public String createWorkDir() {
        try {
            // 在共享目录下创建子目录，这样容器池可以访问
            String subDirName = "job-" + UUID.randomUUID().toString();
            File workDir = new File(sharedWorkDir, subDirName);
            if (!workDir.exists()) {
                workDir.mkdirs();
            }
            log.debug("创建工作目录: {}", workDir.getAbsolutePath());
            return workDir.getAbsolutePath();
        } catch (Exception e) {
            log.error("创建工作目录失败", e);
            throw new RuntimeException("创建工作目录失败", e);
        }
    }
    
    /**
     * 从工作目录路径中提取子目录名称（用于容器内路径）
     */
    public String getSubDirName(String workDir) {
        File dir = new File(workDir);
        return dir.getName();
    }
    
    /**
     * 清理工作目录
     */
    public void cleanupWorkDir(String workDir) {
        try {
            File dir = new File(workDir);
            if (dir.exists()) {
                Files.walk(dir.toPath())
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception e) {
                                log.error("删除文件失败: {}", path, e);
                            }
                        });
            }
            log.debug("清理工作目录成功: {}", workDir);
        } catch (Exception e) {
            log.error("清理工作目录失败: {}", workDir, e);
        }
    }
    
    /**
     * Docker执行结果
     */
    @lombok.Builder
    @lombok.Data
    public static class DockerExecuteResult {
        private boolean success;        // 是否执行成功
        private int exitCode;           // 退出码
        private String output;          // 标准输出
        private String error;           // 错误输出
        private long executeTime;       // 执行时间（毫秒）
        private long memoryUsed;        // 内存使用（字节）
    }
    
    /**
     * 日志回调处理器
     */
    private static class LogContainerResultCallback extends com.github.dockerjava.api.async.ResultCallbackTemplate<
            com.github.dockerjava.api.async.ResultCallbackTemplate<LogContainerResultCallback, com.github.dockerjava.api.model.Frame>,
            com.github.dockerjava.api.model.Frame> {
        
        private final ByteArrayOutputStream stdout;
        private final ByteArrayOutputStream stderr;
        
        public LogContainerResultCallback(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }
        
        @Override
        public void onNext(com.github.dockerjava.api.model.Frame frame) {
            try {
                switch (frame.getStreamType()) {
                    case STDOUT:
                    case RAW:
                        stdout.write(frame.getPayload());
                        break;
                    case STDERR:
                        stderr.write(frame.getPayload());
                        break;
                }
            } catch (Exception e) {
                // 忽略
            }
        }
    }
    
    /**
     * 镜像拉取回调
     */
    private static class PullImageResultCallback extends com.github.dockerjava.api.command.PullImageResultCallback {
        @Override
        public void onNext(com.github.dockerjava.api.model.PullResponseItem item) {
            // 可以在这里打印拉取进度
            super.onNext(item);
        }
    }
}
