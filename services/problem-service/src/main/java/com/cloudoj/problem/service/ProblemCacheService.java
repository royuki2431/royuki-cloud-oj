package com.cloudoj.problem.service;

import com.cloudoj.model.entity.problem.Problem;

import java.util.List;
import java.util.Map;

/**
 * 题目缓存服务接口
 */
public interface ProblemCacheService {
    
    /**
     * 获取热门题目列表（按提交次数排序）
     */
    List<Problem> getHotProblems(int limit);
    
    /**
     * 刷新热门题目缓存
     */
    void refreshHotProblems();
    
    /**
     * 获取题目详情（带缓存）
     */
    Problem getProblemById(Long id);
    
    /**
     * 清除题目缓存
     */
    void clearProblemCache(Long id);
    
    /**
     * 增加题目访问量
     */
    void incrementViewCount(Long problemId);
    
    /**
     * 获取题目访问量排行
     */
    List<Map<String, Object>> getViewRanking(int limit);
    
    /**
     * 缓存题目统计信息
     */
    void cacheProblemStats(Long problemId, int submitCount, int acceptCount);
    
    /**
     * 获取缓存的题目统计信息
     */
    Map<String, Object> getProblemStats(Long problemId);
}
