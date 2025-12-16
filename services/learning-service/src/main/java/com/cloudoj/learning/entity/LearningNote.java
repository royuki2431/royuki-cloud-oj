package com.cloudoj.learning.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学习笔记实体类
 */
@Data
public class LearningNote implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 笔记ID
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
     * 笔记标题
     */
    private String title;
    
    /**
     * 笔记内容
     */
    private String content;
    
    /**
     * 是否公开：1-是 0-否
     */
    private Integer isPublic;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
