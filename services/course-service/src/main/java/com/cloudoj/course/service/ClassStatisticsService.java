package com.cloudoj.course.service;

import java.util.List;
import java.util.Map;

/**
 * 班级统计服务接口
 */
public interface ClassStatisticsService {
    
    /**
     * 获取班级学情统计
     */
    Map<String, Object> getClassStatistics(Long classId);
    
    /**
     * 获取班级作业完成情况
     */
    Map<String, Object> getHomeworkCompletion(Long classId, Long homeworkId);
    
    /**
     * 获取班级学生排名（作业总分）
     */
    List<Map<String, Object>> getStudentRanking(Long classId);
    
    /**
     * 获取单个作业排名
     */
    List<Map<String, Object>> getHomeworkRanking(Long homeworkId, Long classId);
    
    /**
     * 获取题库训练排名
     */
    List<Map<String, Object>> getPracticeRanking(Long classId);
    
    /**
     * 获取课程整体统计
     */
    Map<String, Object> getCourseStatistics(Long courseId);
    
    /**
     * 记录每日班级统计
     */
    void recordDailyStatistics(Long classId);
}
