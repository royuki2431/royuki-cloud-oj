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
 * Python语言沙箱
 * Docker镜像：python:3.13-slim
 */
@Slf4j
@Component
public class PythonSandbox implements LanguageSandbox {
    
    @Autowired
    private DockerSandbox dockerSandbox;
    
    private static final String DOCKER_IMAGE = "python:3.13-slim";
    private static final String SOURCE_FILE = "main.py";
    
    @Override
    public JudgeResult judge(String code, List<JudgeTestCase> testCases, int timeLimit, int memoryLimit) {
        String workDir = null;
        
        try {
            // 创建工作目录
            workDir = dockerSandbox.createWorkDir();
            log.info("Python评测开始, workDir={}", workDir);
            
            // 写入代码文件
            String codeFile = workDir + File.separator + SOURCE_FILE;
            Files.writeString(Paths.get(codeFile), code);
            
            log.debug("Python代码已写入，开始执行测试用例");
            
            // 执行测试用例（Python无需编译）
            List<TestCaseResult> testCaseResults = new ArrayList<>();
            int passedCount = 0;
            long totalTime = 0;
            long maxMemory = 0;
            String firstError = null;
            
            for (JudgeTestCase testCase : testCases) {
                // 写入输入数据
                String inputFile = workDir + File.separator + "input.txt";
                Files.writeString(Paths.get(inputFile), testCase.getInput());
                
                // 运行代码
                DockerSandbox.DockerExecuteResult runResult = run(workDir, timeLimit);
                
                totalTime += runResult.getExecuteTime();
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
            
            log.info("Python评测完成: status={}, score={}, passed={}/{}", 
                    status, score, passedCount, testCases.size());
            
            return JudgeResult.builder()
                    .success(passedCount == testCases.size())
                    .status(status)
                    .score(score)
                    .timeUsed(totalTime)
                    .memoryUsed(maxMemory)
                    .errorMessage(passedCount == testCases.size() ? null : firstError)
                    .passedTestCases(passedCount)
                    .totalTestCases(testCases.size())
                    .testCaseResults(testCaseResults)
                    .build();
            
        } catch (Exception e) {
            log.error("Python评测异常", e);
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
     * 运行Python代码
     */
    private DockerSandbox.DockerExecuteResult run(String workDir, int timeLimit) {
        String[] command = new String[]{
                "sh", "-c",
                "timeout " + (timeLimit / 1000) + "s python3 " + SOURCE_FILE + " < input.txt 2>&1"
        };
        
        return dockerSandbox.execute(DOCKER_IMAGE, command, workDir, timeLimit / 1000 + 1);
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
