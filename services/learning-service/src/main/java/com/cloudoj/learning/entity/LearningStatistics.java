package com.cloudoj.learning.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习统计实体类
 */
@Data
public class LearningStatistics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 统计ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 统计日期
     */
    private LocalDate statDate;
    
    /**
     * 提交次数
     */
    private Integer submitCount;
    
    /**
     * 通过次数
     */
    private Integer acceptCount;
    
    /**
     * 解决题目数
     */
    private Integer problemSolved;
    
    /**
     * 代码行数
     */
    private Integer codeLines;
    
    /**
     * 学习时长（分钟）
     */
    private Integer studyDuration;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
