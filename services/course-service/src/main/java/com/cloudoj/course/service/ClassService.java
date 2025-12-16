package com.cloudoj.course.service;

import com.cloudoj.course.entity.CourseClass;

import java.util.List;

/**
 * 班级服务接口
 */
public interface ClassService {
    
    /**
     * 创建班级（自动生成邀请码）
     */
    Long createClass(CourseClass courseClass);
    
    /**
     * 根据ID查询班级
     */
    CourseClass getClassById(Long id);
    
    /**
     * 根据邀请码查询班级
     */
    CourseClass getClassByCode(String code);
    
    /**
     * 查询课程的班级列表
     */
    List<CourseClass> getCourseClasses(Long courseId);
    
    /**
     * 学生加入班级
     */
    void joinClass(Long studentId, String inviteCode);
    
    /**
     * 学生退出班级
     */
    void leaveClass(Long studentId, Long classId);
    
    /**
     * 移除学生
     */
    void removeStudent(Long classId, Long studentId);
    
    /**
     * 获取班级学生列表
     */
    java.util.List<java.util.Map<String, Object>> getClassStudents(Long classId);
    
    /**
     * 获取学生加入的班级列表
     */
    java.util.List<CourseClass> getStudentClasses(Long studentId);
    
    /**
     * 获取学生加入的班级详细列表（包含课程和教师信息）
     */
    java.util.List<java.util.Map<String, Object>> getStudentClassesWithDetail(Long studentId);
    
    /**
     * 检查学生是否在班级中
     */
    boolean isStudentInClass(Long studentId, Long classId);
    
    /**
     * 更新班级信息
     */
    void updateClass(CourseClass courseClass);
    
    /**
     * 删除班级
     */
    void deleteClass(Long id);
    
    /**
     * 同步所有班级和课程的学生数量
     */
    void syncAllStudentCounts();
}
