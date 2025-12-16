package com.cloudoj.learning.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学习进度实体类
 */
@Data
public class LearningProgress implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 进度ID
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
     * 状态：NOT_STARTED/IN_PROGRESS/COMPLETED
     */
    private String status;
    
    /**
     * 提交次数
     */
    private Integer submitCount;
    
    /**
     * 通过次数
     */
    private Integer acceptCount;
    
    /**
     * 首次提交时间
     */
    private LocalDateTime firstSubmitTime;
    
    /**
     * 最后提交时间
     */
    private LocalDateTime lastSubmitTime;
    
    /**
     * 首次通过时间
     */
    private LocalDateTime firstAcceptTime;
    
    /**
     * 最高得分
     */
    private Integer bestScore;
    
    /**
     * 最佳执行时间（毫秒）
     */
    private Integer executionTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
