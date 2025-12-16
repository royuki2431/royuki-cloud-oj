package com.cloudoj.course.service;

import com.cloudoj.course.entity.Course;

import java.util.List;

/**
 * 课程服务接口
 */
public interface CourseService {
    
    /**
     * 创建课程
     */
    Long createCourse(Course course);
    
    /**
     * 根据ID查询课程
     */
    Course getCourseById(Long id);
    
    /**
     * 查询教师的课程列表
     */
    List<Course> getTeacherCourses(Long teacherId);
    
    /**
     * 查询学生的课程列表
     */
    List<Course> getStudentCourses(Long studentId);
    
    /**
     * 更新课程信息
     */
    void updateCourse(Course course);
    
    /**
     * 删除课程
     */
    void deleteCourse(Long id);
}
