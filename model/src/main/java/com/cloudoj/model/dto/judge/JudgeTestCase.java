package com.cloudoj.model.dto.judge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评测测试用例DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeTestCase {
    
    /**
     * 测试用例ID
     */
    private Long id;
    
    /**
     * 输入数据
     */
    private String input;
    
    /**
     * 预期输出数据
     */
    private String output;
    
    /**
     * 时间限制（毫秒）
     */
    private Integer timeLimit;
    
    /**
     * 内存限制（MB）
     */
    private Integer memoryLimit;
    
    /**
     * 测试用例分值
     */
    private Integer score;
}
