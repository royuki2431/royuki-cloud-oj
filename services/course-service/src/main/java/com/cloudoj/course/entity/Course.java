package com.cloudoj.course.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 课程实体类
 */
@Data
public class Course implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 课程ID
     */
    private Long id;
    
    /**
     * 课程名称
     */
    private String name;
    
    /**
     * 课程描述
     */
    private String description;
    
    /**
     * 封面图片
     */
    private String coverImage;
    
    /**
     * 授课教师ID
     */
    private Long teacherId;
    
    /**
     * 学期
     */
    private String semester;
    
    /**
     * 学年
     */
    private Integer year;
    
    /**
     * 状态：1-进行中 0-已结束
     */
    private Integer status;
    
    /**
     * 学生人数
     */
    private Integer studentCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
