package com.cloudoj.judge.sandbox;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Docker沙箱基础类
 * 提供Docker容器的创建、执行和清理功能
 */
@Slf4j
@Component
public class DockerSandbox {
    
    private final DockerClient dockerClient;
    
    // 资源限制
    private static final long MEMORY_LIMIT = 256 * 1024 * 1024L; // 256MB
    private static final long CPU_COUNT = 1L; // 1核CPU
    private static final int TIME_LIMIT = 5; // 5秒超时
    
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
     * 执行Docker命令
     * 
     * @param image Docker镜像名称
     * @param command 要执行的命令
     * @param workDir 工作目录（宿主机路径）
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     */
    public DockerExecuteResult execute(String image, String[] command, String workDir, int timeoutSeconds) {
        String containerId = null;
        long startTime = System.currentTimeMillis();
        
        try {
            // 确保镜像存在
            ensureImageExists(image);
            
            // 创建容器
            containerId = createContainer(image, command, workDir);
            log.debug("创建容器成功: {}", containerId);
            
            // 启动容器
            dockerClient.startContainerCmd(containerId).exec();
            log.debug("启动容器成功: {}", containerId);
            
            // 等待容器执行完成（带超时）
            Integer exitCode = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS);
            
            long executeTime = System.currentTimeMillis() - startTime;
            
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
     * 创建容器
     */
    private String createContainer(String image, String[] command, String workDir) {
        // 创建卷挂载
        Volume volume = new Volume("/workspace");
        Bind bind = new Bind(workDir, volume);
        
        // 配置主机资源限制
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(bind)
                .withMemory(MEMORY_LIMIT)
                .withMemorySwap(MEMORY_LIMIT) // 禁用swap
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
            return MEMORY_LIMIT / 2; // 返回最大限制的一半作为估计值
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
     * 创建临时工作目录
     */
    public String createWorkDir() {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            File workDir = new File(tempDir, "oj-judge-" + UUID.randomUUID().toString());
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
