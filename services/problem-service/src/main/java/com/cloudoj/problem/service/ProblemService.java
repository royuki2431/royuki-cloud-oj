package com.cloudoj.problem.service;

import com.cloudoj.model.entity.problem.Problem;

import java.util.List;

/**
 * 题目服务接口
 */
public interface ProblemService {
    
    /**
     * 根据ID查询题目
     */
    Problem getProblemById(Long id);
    
    /**
     * 查询所有题目（分页）
     */
    List<Problem> getAllProblems(Integer pageNum, Integer pageSize);
    
    /**
     * 根据难度查询题目
     */
    List<Problem> getProblemsByDifficulty(String difficulty, Integer pageNum, Integer pageSize);
    
    /**
     * 根据标题搜索题目
     */
    List<Problem> searchProblemsByTitle(String title, Integer pageNum, Integer pageSize);
    
    /**
     * 创建题目
     */
    Long createProblem(Problem problem);
    
    /**
     * 更新题目
     */
    void updateProblem(Problem problem);
    
    /**
     * 删除题目
     */
    void deleteProblem(Long id);
    
    /**
     * 统计题目总数
     */
    Long getTotalCount();
    
    /**
     * 更新提交统计
     */
    void updateSubmitCount(Long id, Boolean isAccepted);
    
    // ==================== 管理员功能 ====================
    
    /**
     * 获取题目列表（管理员/教师）
     * @param keyword 搜索关键词
     * @param difficulty 难度
     * @param category 分类
     * @param status 状态
     * @param authorId 作者ID，非空时只查询该作者的题目
     * @param offset 偏移量
     * @param limit 数量
     * @return 题目列表
     */
    List<Problem> getProblemListAdmin(String keyword, String difficulty, String category, Integer status, Long authorId, int offset, int limit);
    
    /**
     * 统计题目数量
     * @param keyword 搜索关键词
     * @param difficulty 难度
     * @param category 分类
     * @param status 状态
     * @param authorId 作者ID，非空时只统计该作者的题目
     * @return 题目数量
     */
    int countProblems(String keyword, String difficulty, String category, Integer status, Long authorId);
    
    /**
     * 更新题目状态
     * @param problemId 题目ID
     * @param status 状态
     */
    void updateProblemStatus(Long problemId, Integer status);
}
