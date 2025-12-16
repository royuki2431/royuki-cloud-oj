package com.cloudoj.course.service;

import com.cloudoj.course.entity.Homework;
import com.cloudoj.course.entity.HomeworkProblem;

import java.util.List;
import java.util.Map;

/**
 * 作业服务接口
 */
public interface HomeworkService {
    
    /**
     * 创建作业并关联题目
     * @param homework 作业信息
     * @param problems 题目列表（包含题目ID和分值）
     * @return 作业ID
     */
    Long createHomework(Homework homework, List<HomeworkProblem> problems);
    
    /**
     * 根据ID查询作业详情（包含题目列表）
     */
    Map<String, Object> getHomeworkDetail(Long id);
    
    /**
     * 查询课程的作业列表
     */
    List<Homework> getCourseHomeworks(Long courseId);
    
    /**
     * 查询课程的作业列表（包含统计信息）
     */
    List<Map<String, Object>> getCourseHomeworksWithStats(Long courseId);
    
    /**
     * 查询班级的作业列表
     */
    List<Homework> getClassHomeworks(Long classId);
    
    /**
     * 查询班级的作业列表（包含题目数量）
     */
    List<Map<String, Object>> getClassHomeworksWithProblemCount(Long classId);
    
    /**
     * 查询学生的作业列表（根据学生加入的班级）
     */
    List<Map<String, Object>> getStudentHomeworks(Long studentId);
    
    /**
     * 更新作业信息
     */
    void updateHomework(Homework homework);
    
    /**
     * 更新作业题目列表
     */
    void updateHomeworkProblems(Long homeworkId, List<HomeworkProblem> problems);
    
    /**
     * 删除作业
     */
    void deleteHomework(Long id);
    
    /**
     * 记录学生提交（由judge-service调用）
     * @param studentId 学生ID
     * @param problemId 题目ID
     * @param judgeSubmissionId 评测提交ID
     */
    void recordSubmission(Long studentId, Long problemId, Long judgeSubmissionId);
    
    /**
     * 查询作业完成统计
     */
    Map<String, Object> getHomeworkStatistics(Long homeworkId);
    
    /**
     * 查询学生作业成绩
     */
    Map<String, Object> getStudentHomeworkScore(Long studentId, Long homeworkId);
    
    /**
     * 查询学生的作业详情（包含题目完成情况）
     */
    Map<String, Object> getStudentHomeworkDetail(Long homeworkId, Long studentId);
}
