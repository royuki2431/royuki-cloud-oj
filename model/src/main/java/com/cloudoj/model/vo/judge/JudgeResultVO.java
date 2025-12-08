package com.cloudoj.model.vo.judge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 评测结果视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResultVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 提交ID
     */
    private Long submissionId;
    
    /**
     * 评测状态
     */
    private String status;
    
    /**
     * 评测状态描述
     */
    private String statusDesc;
    
    /**
     * 得分
     */
    private Integer score;
    
    /**
     * 运行时间（毫秒）
     */
    private Integer timeUsed;
    
    /**
     * 内存使用（KB）
     */
    private Integer memoryUsed;
    
    /**
     * 通过率
     */
    private String passRate;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 测试用例结果详情
     */
    private List<TestCaseResultVO> testCaseResults;
    
    /**
     * 测试用例结果VO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResultVO implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * 测试用例ID
         */
        private Long testCaseId;
        
        /**
         * 测试结果
         */
        private String status;
        
        /**
         * 运行时间（毫秒）
         */
        private Long timeUsed;
        
        /**
         * 内存使用（KB）
         */
        private Long memoryUsed;
        
        /**
         * 输入数据
         */
        private String input;
        
        /**
         * 预期输出
         */
        private String expectedOutput;
        
        /**
         * 实际输出
         */
        private String actualOutput;
        
        /**
         * 错误信息
         */
        private String errorMessage;
    }
}
