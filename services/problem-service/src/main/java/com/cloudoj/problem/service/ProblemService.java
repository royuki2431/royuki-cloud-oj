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
}
