package com.cloudoj.model.dto.judge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 评测消息DTO
 * 用于在RabbitMQ中传递评测任务
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 提交ID
     */
    private Long submissionId;
    
    /**
     * 题目ID
     */
    private Long problemId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 编程语言
     */
    private String language;
    
    /**
     * 提交代码
     */
    private String code;
    
    /**
     * 重试次数
     */
    private Integer retryCount = 0;
}
