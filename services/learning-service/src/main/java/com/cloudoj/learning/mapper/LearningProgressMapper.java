package com.cloudoj.learning.mapper;

import com.cloudoj.learning.entity.LearningProgress;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学习进度 Mapper 接口
 */
public interface LearningProgressMapper {
    
    /**
     * 根据主键查询
     */
    LearningProgress selectByPrimaryKey(Long id);
    
    /**
     * 根据用户ID和题目ID查询
     */
    LearningProgress selectByUserIdAndProblemId(@Param("userId") Long userId, @Param("problemId") Long problemId);
    
    /**
     * 根据用户ID查询学习进度列表
     */
    List<LearningProgress> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID和状态查询
     */
    List<LearningProgress> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);
    
    /**
     * 插入学习进度
     */
    int insert(LearningProgress record);
    
    /**
     * 选择性插入
     */
    int insertSelective(LearningProgress record);
    
    /**
     * 选择性更新
     */
    int updateByPrimaryKeySelective(LearningProgress record);
    
    /**
     * 更新学习进度（用于提交后更新）
     */
    int updateProgress(LearningProgress record);
    
    /**
     * 统计用户解决题目数
     */
    Long countCompletedByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户各难度解决题目数
     */
    List<java.util.Map<String, Object>> countByUserIdGroupByDifficulty(@Param("userId") Long userId);
    
    /**
     * 获取题目总数
     */
    Long countTotalProblems();
    
    /**
     * 按难度统计题目总数
     */
    java.util.Map<String, Object> countProblemsByDifficulty();
    
    /**
     * 计算用户连续学习天数
     */
    int calculateStreak(@Param("userId") Long userId);
    
    /**
     * 计算用户总训练得分（每道题取最高分之和）
     */
    Long sumBestScoreByUserId(@Param("userId") Long userId);
}
