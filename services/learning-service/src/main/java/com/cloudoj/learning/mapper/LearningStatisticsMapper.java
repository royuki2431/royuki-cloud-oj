package com.cloudoj.learning.mapper;

import com.cloudoj.learning.entity.LearningStatistics;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 学习统计 Mapper 接口
 */
public interface LearningStatisticsMapper {
    
    /**
     * 根据主键查询
     */
    LearningStatistics selectByPrimaryKey(Long id);
    
    /**
     * 根据用户ID和日期查询
     */
    LearningStatistics selectByUserAndDate(@Param("userId") Long userId, @Param("statDate") LocalDate statDate);
    
    /**
     * 查询用户指定日期范围的统计数据
     */
    List<LearningStatistics> selectByUserAndDateRange(@Param("userId") Long userId,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);
    
    /**
     * 查询用户总统计数据
     */
    java.util.Map<String, Object> selectTotalsByUserId(@Param("userId") Long userId);
    
    /**
     * 查询排行榜
     */
    java.util.List<java.util.Map<String, Object>> selectLeaderboard(@Param("limit") int limit);
    
    /**
     * 插入统计数据
     */
    int insert(LearningStatistics record);
    
    /**
     * 选择性插入
     */
    int insertSelective(LearningStatistics record);
    
    /**
     * 选择性更新
     */
    int updateByPrimaryKeySelective(LearningStatistics record);
    
    /**
     * 更新统计数据（增量更新）
     */
    int updateStatistics(LearningStatistics record);
}
