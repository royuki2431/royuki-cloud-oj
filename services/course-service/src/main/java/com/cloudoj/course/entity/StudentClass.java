package com.cloudoj.course.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生-班级关联实体类
 */
@Data
public class StudentClass implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 关联ID
     */
    private Long id;
    
    /**
     * 学生ID
     */
    private Long studentId;
    
    /**
     * 班级ID
     */
    private Long classId;
    
    /**
     * 加入时间
     */
    private LocalDateTime joinTime;
    
    /**
     * 状态：1-正常 0-退出
     */
    private Integer status;
}
