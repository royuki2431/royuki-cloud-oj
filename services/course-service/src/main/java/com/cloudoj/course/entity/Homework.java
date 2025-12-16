package com.cloudoj.course.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 作业实体类
 */
@Data
public class Homework implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 作业ID
     */
    private Long id;
    
    /**
     * 课程ID
     */
    private Long courseId;
    
    /**
     * 班级ID（可为空，为空则全课程）
     */
    private Long classId;
    
    /**
     * 作业标题
     */
    private String title;
    
    /**
     * 作业描述
     */
    private String description;
    
    /**
     * 布置教师ID
     */
    private Long teacherId;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 截止时间
     */
    private LocalDateTime endTime;
    
    /**
     * 总分
     */
    private Integer totalScore;
    
    /**
     * 作业状态：1-进行中 0-已结束
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
