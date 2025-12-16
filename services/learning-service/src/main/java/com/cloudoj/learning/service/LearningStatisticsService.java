package com.cloudoj.learning.service;

import com.cloudoj.learning.entity.LearningStatistics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 学习统计服务接口
 */
public interface LearningStatisticsService {
    
    /**
     * 记录今日学习数据
     */
    void recordDailyStatistics(Long userId, int submitCount, int acceptCount, int problemSolved, int codeLines);
    
    /**
     * 获取用户某日的学习统计
     */
    LearningStatistics getDailyStatistics(Long userId, LocalDate date);
    
    /**
     * 获取用户一段时间的学习统计
     */
    List<LearningStatistics> getStatisticsRange(Long userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取用户学习热力图数据（支持日期范围）
     */
    List<Map<String, Object>> getHeatmapData(Long userId, String startDate, String endDate);
    
    /**
     * 获取用户学习总览
     */
    Map<String, Object> getLearningOverview(Long userId);
    
    /**
     * 获取用户排行榜
     */
    List<Map<String, Object>> getLeaderboard(int limit);
    
    /**
     * 增加提交次数
     */
    void incrementSubmitCount(Long userId);
    
    /**
     * 增加通过次数
     */
    void incrementAcceptCount(Long userId);
}
