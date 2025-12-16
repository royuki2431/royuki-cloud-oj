package com.cloudoj.learning.service;

import com.cloudoj.learning.entity.LearningProgress;

import java.util.List;
import java.util.Map;

/**
 * 学习进度服务接口
 */
public interface LearningProgressService {
    
    /**
     * 获取用户学习进度
     */
    LearningProgress getProgress(Long userId, Long problemId);
    
    /**
     * 获取用户所有学习进度
     */
    List<LearningProgress> getUserProgressList(Long userId);
    
    /**
     * 获取用户指定状态的学习进度
     */
    List<LearningProgress> getUserProgressByStatus(Long userId, String status);
    
    /**
     * 更新学习进度（提交后调用）
     */
    void updateProgress(Long userId, Long problemId, String status, Integer score, Integer executionTime);
    
    /**
     * 获取学习统计摘要
     */
    Map<String, Object> getProgressSummary(Long userId);
}
