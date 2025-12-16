package com.cloudoj.judge.sandbox;

import com.cloudoj.model.dto.judge.JudgeTestCase;
import com.cloudoj.model.enums.JudgeStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * C++语言沙箱
 * Docker镜像：gcc:latest
 * 支持容器池模式，复用容器减少开销
 */
@Slf4j
@Component
public class CppSandbox implements LanguageSandbox {
    
    @Autowired
    private DockerSandbox dockerSandbox;
    
    private static final String DOCKER_IMAGE = "gcc:latest";
    private static final String SOURCE_FILE = "main.cpp";
    private static final String EXECUTABLE_FILE = "main";
    
    @Override
    public JudgeResult judge(String code, List<JudgeTestCase> testCases, int timeLimit, int memoryLimit) {
        String workDir = null;
        String subDir = null;
        
        try {
            // 创建工作目录（在共享目录下创建子目录）
            workDir = dockerSandbox.createWorkDir();
            subDir = dockerSandbox.getSubDirName(workDir);
            log.info("C++评测开始, workDir={}, subDir={}, timeLimit={}ms, memoryLimit={}MB", 
                    workDir, subDir, timeLimit, memoryLimit);
            
            // 获取或创建容器池中的容器（挂载共享目录）
            String pooledContainerId = dockerSandbox.getOrCreatePooledContainer(DOCKER_IMAGE);
            boolean usePooledContainer = (pooledContainerId != null);
            
            // 写入代码文件
            String codeFile = workDir + File.separator + SOURCE_FILE;
            Files.writeString(Paths.get(codeFile), code);
            
            // 编译代码
            DockerSandbox.DockerExecuteResult compileResult = usePooledContainer
                    ? compileInContainer(pooledContainerId, subDir, timeLimit)
                    : compile(workDir, timeLimit, memoryLimit);
            if (!compileResult.isSuccess()) {
                log.warn("编译失败: {}", compileResult.getError());
                return JudgeResult.builder()
                        .success(false)
                        .status(JudgeStatusEnum.COMPILE_ERROR.getCode())
                        .score(0)
                        .timeUsed(compileResult.getExecuteTime())
                        .memoryUsed(0)
                        .errorMessage("编译错误: " + compileResult.getError())
                        .passedTestCases(0)
                        .totalTestCases(testCases.size())
                        .build();
            }
            
            log.debug("编译成功，开始执行测试用例");
            
            // 执行测试用例
            List<TestCaseResult> testCaseResults = new ArrayList<>();
            int passedCount = 0;
            long maxTime = 0;  // 记录最大运行时间（不包括编译时间）
            long maxMemory = 0;
            String firstError = null;
            
            for (JudgeTestCase testCase : testCases) {
                // 写入输入数据
                String inputFile = workDir + File.separator + "input.txt";
                Files.writeString(Paths.get(inputFile), testCase.getInput());
                
                // 运行代码（优先使用容器池）
                DockerSandbox.DockerExecuteResult runResult = usePooledContainer
                        ? runInContainer(pooledContainerId, subDir, timeLimit)
                        : run(workDir, timeLimit, memoryLimit);
                
                maxTime = Math.max(maxTime, runResult.getExecuteTime());  // 取最大运行时间
                maxMemory = Math.max(maxMemory, runResult.getMemoryUsed() / 1024);
                
                // 检查运行结果
                TestCaseResult testCaseResult = processTestCase(testCase, runResult);
                testCaseResults.add(testCaseResult);
                
                if (testCaseResult.isPassed()) {
                    passedCount++;
                } else if (firstError == null) {
                    firstError = testCaseResult.getErrorMessage();
                }
                
                if (!runResult.isSuccess() && runResult.getExitCode() != 0) {
                    break;
                }
            }
            
            // 计算得分和状态
            int score = (int) (100.0 * passedCount / testCases.size());
            String status = determineStatus(passedCount, testCases.size(), firstError);
            
            log.info("C++评测完成: status={}, score={}, passed={}/{}", 
                    status, score, passedCount, testCases.size());
            
            return JudgeResult.builder()
                    .success(passedCount == testCases.size())
                    .status(status)
                    .score(score)
                    .timeUsed(maxTime)
                    .memoryUsed(maxMemory)
                    .errorMessage(passedCount == testCases.size() ? null : firstError)
                    .passedTestCases(passedCount)
                    .totalTestCases(testCases.size())
                    .testCaseResults(testCaseResults)
                    .build();
            
        } catch (Exception e) {
            log.error("C++评测异常", e);
            return JudgeResult.builder()
                    .success(false)
                    .status(JudgeStatusEnum.SYSTEM_ERROR.getCode())
                    .score(0)
                    .errorMessage("系统错误: " + e.getMessage())
                    .passedTestCases(0)
                    .totalTestCases(testCases.size())
                    .build();
        } finally {
            if (workDir != null) {
                dockerSandbox.cleanupWorkDir(workDir);
            }
        }
    }
    
    /**
     * 编译C++代码（传统模式）
     */
    private DockerSandbox.DockerExecuteResult compile(String workDir, int timeLimit, int memoryLimit) {
        String[] command = new String[]{
                "sh", "-c",
                "g++ -std=c++17 -O2 -o " + EXECUTABLE_FILE + " " + SOURCE_FILE + " 2>&1"
        };
        
        return dockerSandbox.execute(DOCKER_IMAGE, command, workDir, timeLimit / 1000, memoryLimit);
    }
    
    /**
     * 编译C++代码（容器池模式）
     */
    private DockerSandbox.DockerExecuteResult compileInContainer(String containerId, String subDir, int timeLimit) {
        String[] command = new String[]{
                "sh", "-c",
                "g++ -std=c++17 -O2 -o " + EXECUTABLE_FILE + " " + SOURCE_FILE + " 2>&1"
        };
        
        return dockerSandbox.executeInContainer(containerId, command, subDir, timeLimit / 1000);
    }
    
    /**
     * 运行C++代码（传统模式）
     */
    private DockerSandbox.DockerExecuteResult run(String workDir, int timeLimit, int memoryLimit) {
        String[] command = new String[]{
                "sh", "-c",
                "timeout " + (timeLimit / 1000) + "s ./" + EXECUTABLE_FILE + " < input.txt 2>&1"
        };
        
        return dockerSandbox.execute(DOCKER_IMAGE, command, workDir, timeLimit / 1000 + 1, memoryLimit);
    }
    
    /**
     * 运行C++代码（容器池模式）
     */
    private DockerSandbox.DockerExecuteResult runInContainer(String containerId, String subDir, int timeLimit) {
        String[] command = new String[]{
                "sh", "-c",
                "timeout " + (timeLimit / 1000) + "s ./" + EXECUTABLE_FILE + " < input.txt 2>&1"
        };
        
        return dockerSandbox.executeInContainer(containerId, command, subDir, timeLimit / 1000 + 1);
    }
    
    private TestCaseResult processTestCase(JudgeTestCase testCase, DockerSandbox.DockerExecuteResult runResult) {
        String actualOutput = runResult.getOutput().trim();
        String expectedOutput = testCase.getOutput().trim();
        boolean passed = actualOutput.equals(expectedOutput);
        
        String errorMessage = null;
        if (!passed) {
            if (runResult.getExitCode() == 124) {
                errorMessage = "运行超时";
            } else if (runResult.getExitCode() != 0) {
                errorMessage = "运行错误: " + runResult.getError();
            } else {
                errorMessage = "答案错误";
            }
        }
        
        return TestCaseResult.builder()
                .testCaseId(testCase.getId())
                .passed(passed)
                .actualOutput(actualOutput.length() > 1000 ? actualOutput.substring(0, 1000) + "..." : actualOutput)
                .expectedOutput(expectedOutput)
                .timeUsed(runResult.getExecuteTime())
                .memoryUsed(runResult.getMemoryUsed() / 1024)
                .errorMessage(errorMessage)
                .build();
    }
    
    private String determineStatus(int passedCount, int totalCount, String firstError) {
        if (passedCount == totalCount) {
            return JudgeStatusEnum.ACCEPTED.getCode();
        } else if (firstError != null && firstError.contains("超时")) {
            return JudgeStatusEnum.TIME_LIMIT_EXCEEDED.getCode();
        } else if (firstError != null && firstError.contains("运行错误")) {
            return JudgeStatusEnum.RUNTIME_ERROR.getCode();
        } else {
            return JudgeStatusEnum.WRONG_ANSWER.getCode();
        }
    }
}
