package com.cloudoj.learning.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 错题记录实体类
 */
@Data
public class WrongProblem implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 记录ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 题目ID
     */
    private Long problemId;
    
    /**
     * 提交ID
     */
    private Long submissionId;
    
    /**
     * 错误类型
     */
    private String errorType;
    
    /**
     * 错误次数
     */
    private Integer wrongCount;
    
    /**
     * 是否已解决：1-是 0-否
     */
    private Integer isResolved;
    
    /**
     * 解决时间
     */
    private LocalDateTime resolvedTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
