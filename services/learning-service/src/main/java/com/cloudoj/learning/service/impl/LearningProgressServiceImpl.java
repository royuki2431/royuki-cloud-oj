package com.cloudoj.learning.service.impl;

import com.cloudoj.learning.entity.LearningProgress;
import com.cloudoj.learning.mapper.LearningProgressMapper;
import com.cloudoj.learning.service.LearningProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习进度服务实现类
 */
@Slf4j
@Service
public class LearningProgressServiceImpl implements LearningProgressService {
    
    @Autowired
    private LearningProgressMapper learningProgressMapper;
    
    @Override
    public LearningProgress getProgress(Long userId, Long problemId) {
        return learningProgressMapper.selectByUserIdAndProblemId(userId, problemId);
    }
    
    @Override
    public List<LearningProgress> getUserProgressList(Long userId) {
        return learningProgressMapper.selectByUserId(userId);
    }
    
    @Override
    public List<LearningProgress> getUserProgressByStatus(Long userId, String status) {
        return learningProgressMapper.selectByUserIdAndStatus(userId, status);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long userId, Long problemId, String status, Integer score, Integer executionTime) {
        // 查询是否已有进度记录
        LearningProgress progress = learningProgressMapper.selectByUserIdAndProblemId(userId, problemId);
        
        if (progress == null) {
            // 首次提交，创建进度记录
            progress = new LearningProgress();
            progress.setUserId(userId);
            progress.setProblemId(problemId);
            progress.setStatus("IN_PROGRESS");
            progress.setSubmitCount(1);
            progress.setAcceptCount("ACCEPTED".equals(status) ? 1 : 0);
            progress.setFirstSubmitTime(LocalDateTime.now());
            progress.setLastSubmitTime(LocalDateTime.now());
            progress.setBestScore(score != null ? score : 0);
            progress.setExecutionTime(executionTime);
            
            if ("ACCEPTED".equals(status)) {
                progress.setStatus("COMPLETED");
                progress.setFirstAcceptTime(LocalDateTime.now());
            }
            
            learningProgressMapper.insertSelective(progress);
            log.info("创建学习进度记录: userId={}, problemId={}", userId, problemId);
        } else {
            // 更新已有进度
            progress.setLastSubmitTime(LocalDateTime.now());
            
            // 更新最高得分
            if (score != null && score > progress.getBestScore()) {
                progress.setBestScore(score);
            }
            
            // 更新最佳执行时间（取最小值）
            if (executionTime != null) {
                if (progress.getExecutionTime() == null || executionTime < progress.getExecutionTime()) {
                    progress.setExecutionTime(executionTime);
                }
            }
            
            // 如果通过，更新通过次数和状态
            if ("ACCEPTED".equals(status)) {
                progress.setAcceptCount(progress.getAcceptCount() + 1);
                
                // 首次通过
                if (progress.getFirstAcceptTime() == null) {
                    progress.setFirstAcceptTime(LocalDateTime.now());
                    progress.setStatus("COMPLETED");
                }
            }
            
            learningProgressMapper.updateProgress(progress);
            log.info("更新学习进度: userId={}, problemId={}, status={}", userId, problemId, status);
        }
    }
    
    @Override
    public Map<String, Object> getProgressSummary(Long userId) {
        Map<String, Object> summary = new HashMap<>();
        
        // 统计解决题目数
        Long solvedCount = learningProgressMapper.countCompletedByUserId(userId);
        summary.put("solvedCount", solvedCount);
        
        // 获取题目总数（从problem服务获取，这里暂时用固定值或从统计表获取）
        Long totalProblems = learningProgressMapper.countTotalProblems();
        summary.put("totalProblems", totalProblems != null ? totalProblems : 100);
        
        // 按难度统计
        List<Map<String, Object>> difficultyStats = learningProgressMapper.countByUserIdGroupByDifficulty(userId);
        
        // 转换为前端需要的格式
        int easySolved = 0, mediumSolved = 0, hardSolved = 0;
        for (Map<String, Object> stat : difficultyStats) {
            String difficulty = (String) stat.get("difficulty");
            Number count = (Number) stat.get("count");
            if ("EASY".equals(difficulty)) {
                easySolved = count.intValue();
            } else if ("MEDIUM".equals(difficulty)) {
                mediumSolved = count.intValue();
            } else if ("HARD".equals(difficulty)) {
                hardSolved = count.intValue();
            }
        }
        summary.put("easySolved", easySolved);
        summary.put("mediumSolved", mediumSolved);
        summary.put("hardSolved", hardSolved);
        
        // 获取各难度题目总数
        Map<String, Object> difficultyTotals = learningProgressMapper.countProblemsByDifficulty();
        if (difficultyTotals != null) {
            summary.put("easyTotal", getNumberValue(difficultyTotals.get("EASY"), 50L));
            summary.put("mediumTotal", getNumberValue(difficultyTotals.get("MEDIUM"), 80L));
            summary.put("hardTotal", getNumberValue(difficultyTotals.get("HARD"), 30L));
        } else {
            summary.put("easyTotal", 50L);
            summary.put("mediumTotal", 80L);
            summary.put("hardTotal", 30L);
        }
        
        // 查询所有进度
        List<LearningProgress> progressList = learningProgressMapper.selectByUserId(userId);
        
        // 计算总提交数和总通过数
        int totalSubmissions = 0;
        int acceptedCount = 0;
        for (LearningProgress p : progressList) {
            totalSubmissions += p.getSubmitCount();
            acceptedCount += p.getAcceptCount();
        }
        
        summary.put("totalSubmissions", totalSubmissions);
        summary.put("acceptedCount", acceptedCount);
        
        // 计算连续学习天数
        int streak = learningProgressMapper.calculateStreak(userId);
        summary.put("streak", streak);
        
        return summary;
    }
    
    /**
     * 安全获取数值
     */
    private Long getNumberValue(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
