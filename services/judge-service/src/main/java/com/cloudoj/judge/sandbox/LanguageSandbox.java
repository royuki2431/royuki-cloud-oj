package com.cloudoj.judge.sandbox;

import com.cloudoj.model.dto.judge.JudgeTestCase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 语言沙箱接口
 */
public interface LanguageSandbox {
    
    /**
     * 执行代码评测
     * 
     * @param code 代码内容
     * @param testCases 测试用例列表
     * @param timeLimit 时间限制（毫秒）
     * @param memoryLimit 内存限制（MB）
     * @return 评测结果
     */
    JudgeResult judge(String code, List<JudgeTestCase> testCases, int timeLimit, int memoryLimit);
    
    /**
     * 评测结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class JudgeResult {
        /**
         * 是否通过
         */
        private boolean success;
        
        /**
         * 评测状态码
         */
        private String status;
        
        /**
         * 得分
         */
        private int score;
        
        /**
         * 运行时间（毫秒）
         */
        private long timeUsed;
        
        /**
         * 内存使用（KB）
         */
        private long memoryUsed;
        
        /**
         * 错误信息
         */
        private String errorMessage;
        
        /**
         * 通过的测试用例数
         */
        private int passedTestCases;
        
        /**
         * 总测试用例数
         */
        private int totalTestCases;
        
        /**
         * 测试用例结果详情
         */
        private List<TestCaseResult> testCaseResults;
    }
    
    /**
     * 单个测试用例结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class TestCaseResult {
        /**
         * 测试用例ID
         */
        private Long testCaseId;
        
        /**
         * 是否通过
         */
        private boolean passed;
        
        /**
         * 实际输出
         */
        private String actualOutput;
        
        /**
         * 预期输出
         */
        private String expectedOutput;
        
        /**
         * 运行时间（毫秒）
         */
        private long timeUsed;
        
        /**
         * 内存使用（KB）
         */
        private long memoryUsed;
        
        /**
         * 错误信息
         */
        private String errorMessage;
    }
}
