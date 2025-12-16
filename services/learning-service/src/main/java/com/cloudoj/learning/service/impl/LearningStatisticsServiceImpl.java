package com.cloudoj.learning.service.impl;

import com.cloudoj.learning.entity.LearningStatistics;
import com.cloudoj.learning.feign.UserServiceClient;
import com.cloudoj.learning.mapper.LearningProgressMapper;
import com.cloudoj.learning.mapper.LearningStatisticsMapper;
import com.cloudoj.learning.service.LearningStatisticsService;
import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习统计服务实现类
 */
@Slf4j
@Service
public class LearningStatisticsServiceImpl implements LearningStatisticsService {
    
    @Autowired
    private LearningStatisticsMapper learningStatisticsMapper;
    
    @Autowired
    private LearningProgressMapper learningProgressMapper;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Override
    @Transactional
    public void recordDailyStatistics(Long userId, int submitCount, int acceptCount, int problemSolved, int codeLines) {
        LocalDate today = LocalDate.now();
        LearningStatistics existing = learningStatisticsMapper.selectByUserAndDate(userId, today);
        
        if (existing != null) {
            // 更新今日统计
            existing.setSubmitCount(existing.getSubmitCount() + submitCount);
            existing.setAcceptCount(existing.getAcceptCount() + acceptCount);
            existing.setProblemSolved(existing.getProblemSolved() + problemSolved);
            existing.setCodeLines(existing.getCodeLines() + codeLines);
            learningStatisticsMapper.updateByPrimaryKeySelective(existing);
        } else {
            // 创建今日统计
            LearningStatistics stats = new LearningStatistics();
            stats.setUserId(userId);
            stats.setStatDate(today);
            stats.setSubmitCount(submitCount);
            stats.setAcceptCount(acceptCount);
            stats.setProblemSolved(problemSolved);
            stats.setCodeLines(codeLines);
            stats.setStudyDuration(0);
            learningStatisticsMapper.insertSelective(stats);
        }
    }
    
    @Override
    public LearningStatistics getDailyStatistics(Long userId, LocalDate date) {
        return learningStatisticsMapper.selectByUserAndDate(userId, date);
    }
    
    @Override
    public List<LearningStatistics> getStatisticsRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return learningStatisticsMapper.selectByUserAndDateRange(userId, startDate, endDate);
    }
    
    @Override
    public List<Map<String, Object>> getHeatmapData(Long userId, String startDateStr, String endDateStr) {
        // 解析日期参数，如果为空则使用默认值（最近一年）
        LocalDate endDate;
        LocalDate startDate;
        
        if (endDateStr != null && !endDateStr.isEmpty()) {
            endDate = LocalDate.parse(endDateStr);
        } else {
            endDate = LocalDate.now();
        }
        
        if (startDateStr != null && !startDateStr.isEmpty()) {
            startDate = LocalDate.parse(startDateStr);
        } else {
            startDate = endDate.minusYears(1);
        }
        
        log.info("查询热力图数据: userId={}, startDate={}, endDate={}", userId, startDate, endDate);
        
        List<LearningStatistics> stats = learningStatisticsMapper.selectByUserAndDateRange(userId, startDate, endDate);
        log.info("查询到 {} 条统计记录", stats.size());
        
        List<Map<String, Object>> heatmapData = new ArrayList<>();
        for (LearningStatistics stat : stats) {
            Map<String, Object> item = new HashMap<>();
            item.put("statDate", stat.getStatDate().toString());
            item.put("submitCount", stat.getSubmitCount());
            item.put("level", calculateLevel(stat.getSubmitCount()));
            heatmapData.add(item);
            log.debug("热力图数据: date={}, submitCount={}", stat.getStatDate(), stat.getSubmitCount());
        }
        
        return heatmapData;
    }
    
    private int calculateLevel(int count) {
        if (count == 0) return 0;
        if (count <= 2) return 1;
        if (count <= 5) return 2;
        if (count <= 10) return 3;
        return 4;
    }
    
    @Override
    public Map<String, Object> getLearningOverview(Long userId) {
        Map<String, Object> overview = new HashMap<>();
        
        // 获取总统计
        Map<String, Object> totals = learningStatisticsMapper.selectTotalsByUserId(userId);
        if (totals != null) {
            overview.put("totalSubmissions", totals.getOrDefault("totalSubmissions", 0));
            overview.put("totalAccepted", totals.getOrDefault("totalAccepted", 0));
            overview.put("totalCodeLines", totals.getOrDefault("totalCodeLines", 0));
            overview.put("totalStudyDuration", totals.getOrDefault("totalStudyDuration", 0));
        }
        
        // 从LearningProgress获取真正的已解决题目数（题库中已完成的题目）
        Long solvedCount = learningProgressMapper.countCompletedByUserId(userId);
        overview.put("totalProblemsSolved", solvedCount != null ? solvedCount : 0);
        
        // 获取今日统计
        LearningStatistics today = learningStatisticsMapper.selectByUserAndDate(userId, LocalDate.now());
        if (today != null) {
            overview.put("todaySubmissions", today.getSubmitCount());
            overview.put("todayAccepted", today.getAcceptCount());
        } else {
            overview.put("todaySubmissions", 0);
            overview.put("todayAccepted", 0);
        }
        
        // 计算连续学习天数
        int streak = calculateStreak(userId);
        overview.put("streak", streak);
        
        // 计算总训练得分（每道题取最高分之和）
        Long totalTrainingScore = learningProgressMapper.sumBestScoreByUserId(userId);
        overview.put("totalTrainingScore", totalTrainingScore != null ? totalTrainingScore : 0);
        
        return overview;
    }
    
    private int calculateStreak(Long userId) {
        LocalDate date = LocalDate.now();
        int streak = 0;
        
        while (true) {
            LearningStatistics stat = learningStatisticsMapper.selectByUserAndDate(userId, date);
            if (stat != null && stat.getSubmitCount() > 0) {
                streak++;
                date = date.minusDays(1);
            } else {
                break;
            }
        }
        
        return streak;
    }
    
    @Autowired
    private com.cloudoj.learning.feign.JudgeServiceClient judgeServiceClient;
    
    @Override
    public List<Map<String, Object>> getLeaderboard(int limit) {
        List<Map<String, Object>> leaderboard = learningStatisticsMapper.selectLeaderboard(limit);
        
        // 为每个用户添加详细信息
        for (Map<String, Object> item : leaderboard) {
            Long userId = ((Number) item.get("userId")).longValue();
            
            // 从judge-service获取真实的提交统计
            try {
                com.cloudoj.model.common.Result<Map<String, Object>> statsResult = 
                    judgeServiceClient.getUserSubmissionStats(userId);
                if (statsResult != null && statsResult.getData() != null) {
                    Map<String, Object> stats = statsResult.getData();
                    item.put("totalSubmissions", stats.get("totalSubmissions"));
                    item.put("totalAccepted", stats.get("totalAccepted"));
                    item.put("totalProblemsSolved", stats.get("totalProblemsSolved"));
                }
            } catch (Exception e) {
                log.warn("获取用户提交统计失败, userId={}", userId, e);
            }
            
            // 获取用户总分（每道题最高分之和）
            Long totalScore = learningProgressMapper.sumBestScoreByUserId(userId);
            item.put("totalScore", totalScore != null ? totalScore : 0);
            
            try {
                Result<Map<String, Object>> result = userServiceClient.getUserInfo(userId);
                if (result != null && result.getData() != null) {
                    Map<String, Object> userInfo = result.getData();
                    
                    // 获取真实姓名并脱敏（只显示姓氏）
                    String realName = (String) userInfo.get("realName");
                    if (realName != null && !realName.isEmpty()) {
                        String maskedName = maskRealName(realName);
                        item.put("realName", maskedName);
                    } else {
                        item.put("realName", "**");
                    }
                    
                    // 获取学校
                    String school = (String) userInfo.get("school");
                    item.put("school", school != null ? school : "-");
                    
                    // 获取用户名
                    item.put("username", userInfo.get("username"));
                } else {
                    item.put("realName", "**");
                    item.put("school", "-");
                    item.put("username", "用户" + userId);
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败, userId={}", userId, e);
                item.put("realName", "**");
                item.put("school", "-");
                item.put("username", "用户" + userId);
            }
        }
        
        return leaderboard;
    }
    
    /**
     * 脱敏真实姓名，只显示姓氏
     */
    private String maskRealName(String realName) {
        if (realName == null || realName.isEmpty()) {
            return "**";
        }
        // 获取第一个字符（姓氏）
        String surname = realName.substring(0, 1);
        // 其余用*代替
        int length = realName.length();
        StringBuilder masked = new StringBuilder(surname);
        for (int i = 1; i < length; i++) {
            masked.append("*");
        }
        return masked.toString();
    }
    
    @Override
    @Transactional
    public void incrementSubmitCount(Long userId) {
        recordDailyStatistics(userId, 1, 0, 0, 0);
    }
    
    @Override
    @Transactional
    public void incrementAcceptCount(Long userId) {
        recordDailyStatistics(userId, 0, 1, 0, 0);
    }
}
