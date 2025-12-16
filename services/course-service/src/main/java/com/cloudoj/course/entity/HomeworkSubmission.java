package com.cloudoj.course.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 作业提交实体类
 */
@Data
public class HomeworkSubmission implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 提交ID
     */
    private Long id;
    
    /**
     * 作业ID
     */
    private Long homeworkId;
    
    /**
     * 学生ID
     */
    private Long studentId;
    
    /**
     * 题目ID
     */
    private Long problemId;
    
    /**
     * 评测提交ID（关联judge服务的submission表）
     */
    private Long submissionId;
    
    /**
     * 得分
     */
    private Integer score;
    
    /**
     * 评测状态
     */
    private String status;
    
    /**
     * 提交时间
     */
    private LocalDateTime submitTime;
    
    /**
     * 是否迟交：1-是 0-否
     */
    private Integer isLate;
}
