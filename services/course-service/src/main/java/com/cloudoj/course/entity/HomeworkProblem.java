package com.cloudoj.course.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 作业-题目关联实体类
 */
@Data
public class HomeworkProblem implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 关联ID
     */
    private Long id;
    
    /**
     * 作业ID
     */
    private Long homeworkId;
    
    /**
     * 题目ID
     */
    private Long problemId;
    
    /**
     * 分值
     */
    private Integer score;
    
    /**
     * 排序序号
     */
    private Integer orderNum;
}
