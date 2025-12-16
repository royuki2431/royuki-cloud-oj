package com.cloudoj.course.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级实体类
 * 注意：Class是Java关键字，所以使用CourseClass
 */
@Data
public class CourseClass implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 班级ID
     */
    private Long id;
    
    /**
     * 课程ID
     */
    private Long courseId;
    
    /**
     * 班级名称
     */
    private String name;
    
    /**
     * 班级代码（邀请码）
     */
    private String code;
    
    /**
     * 班级描述
     */
    private String description;
    
    /**
     * 教师ID
     */
    private Long teacherId;
    
    /**
     * 最大学生数
     */
    private Integer maxStudents;
    
    /**
     * 当前学生数
     */
    private Integer studentCount;
    
    /**
     * 状态：1-启用 0-禁用
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
