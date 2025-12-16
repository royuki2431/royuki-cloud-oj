package com.cloudoj.problem.service.impl;

import com.cloudoj.model.entity.problem.Problem;
import com.cloudoj.problem.mapper.ProblemMapper;
import com.cloudoj.problem.service.ProblemCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 题目缓存服务实现类
 */
@Slf4j
@Service
public class ProblemCacheServiceImpl implements ProblemCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ProblemMapper problemMapper;
    
    // 缓存 Key 前缀
    private static final String HOT_PROBLEMS_KEY = "problem:hot";
    private static final String PROBLEM_DETAIL_KEY = "problem:detail:";
    private static final String PROBLEM_VIEW_KEY = "problem:view";
    private static final String PROBLEM_STATS_KEY = "problem:stats:";
    
    // 缓存过期时间
    private static final long HOT_PROBLEMS_EXPIRE = 30; // 30分钟
    private static final long PROBLEM_DETAIL_EXPIRE = 60; // 60分钟
    private static final long PROBLEM_STATS_EXPIRE = 10; // 10分钟
    
    /**
     * 服务启动时初始化热门题目缓存
     */
    @PostConstruct
    public void init() {
        try {
            refreshHotProblems();
            log.info("热门题目缓存初始化完成");
        } catch (Exception e) {
            log.warn("热门题目缓存初始化失败，将在首次访问时加载", e);
        }
    }
    
    @Override
    public List<Problem> getHotProblems(int limit) {
        String cacheKey = HOT_PROBLEMS_KEY + ":" + limit;
        
        try {
            // 尝试从缓存获取
            @SuppressWarnings("unchecked")
            List<Problem> cachedProblems = (List<Problem>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedProblems != null && !cachedProblems.isEmpty()) {
                log.debug("从缓存获取热门题目，数量：{}", cachedProblems.size());
                return cachedProblems;
            }
        } catch (Exception e) {
            log.warn("从缓存获取热门题目失败", e);
        }
        
        // 缓存未命中，从数据库查询
        List<Problem> problems = problemMapper.selectHotProblems(limit);
        
        // 写入缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, problems, HOT_PROBLEMS_EXPIRE, TimeUnit.MINUTES);
            log.info("热门题目已缓存，数量：{}", problems.size());
        } catch (Exception e) {
            log.warn("缓存热门题目失败", e);
        }
        
        return problems;
    }
    
    @Override
    @Scheduled(fixedRate = 1800000) // 每30分钟刷新一次
    public void refreshHotProblems() {
        log.info("开始刷新热门题目缓存...");
        
        try {
            // 刷新不同数量的热门题目缓存
            int[] limits = {10, 20, 50};
            for (int limit : limits) {
                List<Problem> problems = problemMapper.selectHotProblems(limit);
                String cacheKey = HOT_PROBLEMS_KEY + ":" + limit;
                redisTemplate.opsForValue().set(cacheKey, problems, HOT_PROBLEMS_EXPIRE, TimeUnit.MINUTES);
            }
            
            log.info("热门题目缓存刷新完成");
        } catch (Exception e) {
            log.error("刷新热门题目缓存失败", e);
        }
    }
    
    @Override
    public Problem getProblemById(Long id) {
        String cacheKey = PROBLEM_DETAIL_KEY + id;
        
        try {
            // 尝试从缓存获取
            Problem cachedProblem = (Problem) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedProblem != null) {
                log.debug("从缓存获取题目详情：id={}", id);
                return cachedProblem;
            }
        } catch (Exception e) {
            log.warn("从缓存获取题目详情失败：id={}", id, e);
        }
        
        // 缓存未命中，从数据库查询
        Problem problem = problemMapper.selectByPrimaryKey(id);
        
        if (problem != null) {
            // 写入缓存
            try {
                redisTemplate.opsForValue().set(cacheKey, problem, PROBLEM_DETAIL_EXPIRE, TimeUnit.MINUTES);
                log.debug("题目详情已缓存：id={}", id);
            } catch (Exception e) {
                log.warn("缓存题目详情失败：id={}", id, e);
            }
        }
        
        return problem;
    }
    
    @Override
    public void clearProblemCache(Long id) {
        try {
            String cacheKey = PROBLEM_DETAIL_KEY + id;
            redisTemplate.delete(cacheKey);
            
            // 同时清除热门题目缓存（因为题目信息可能已更新）
            Set<String> hotKeys = redisTemplate.keys(HOT_PROBLEMS_KEY + ":*");
            if (hotKeys != null && !hotKeys.isEmpty()) {
                redisTemplate.delete(hotKeys);
            }
            
            log.info("已清除题目缓存：id={}", id);
        } catch (Exception e) {
            log.warn("清除题目缓存失败：id={}", id, e);
        }
    }
    
    @Override
    public void incrementViewCount(Long problemId) {
        try {
            // 使用 ZSet 记录访问量
            redisTemplate.opsForZSet().incrementScore(PROBLEM_VIEW_KEY, problemId.toString(), 1);
            log.debug("题目访问量+1：problemId={}", problemId);
        } catch (Exception e) {
            log.warn("增加题目访问量失败：problemId={}", problemId, e);
        }
    }
    
    @Override
    public List<Map<String, Object>> getViewRanking(int limit) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        
        try {
            Set<ZSetOperations.TypedTuple<Object>> tuples = 
                redisTemplate.opsForZSet().reverseRangeWithScores(PROBLEM_VIEW_KEY, 0, limit - 1);
            
            if (tuples != null) {
                int rank = 1;
                for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                    Map<String, Object> item = new HashMap<>();
                    Long problemId = Long.parseLong(tuple.getValue().toString());
                    item.put("rank", rank++);
                    item.put("problemId", problemId);
                    item.put("viewCount", tuple.getScore().longValue());
                    
                    // 获取题目信息
                    Problem problem = getProblemById(problemId);
                    if (problem != null) {
                        item.put("title", problem.getTitle());
                        item.put("difficulty", problem.getDifficulty());
                    }
                    
                    ranking.add(item);
                }
            }
        } catch (Exception e) {
            log.warn("获取题目访问量排行失败", e);
        }
        
        return ranking;
    }
    
    @Override
    public void cacheProblemStats(Long problemId, int submitCount, int acceptCount) {
        try {
            String cacheKey = PROBLEM_STATS_KEY + problemId;
            Map<String, Object> stats = new HashMap<>();
            stats.put("submitCount", submitCount);
            stats.put("acceptCount", acceptCount);
            stats.put("acceptRate", submitCount > 0 ? 
                String.format("%.2f%%", 100.0 * acceptCount / submitCount) : "0.00%");
            stats.put("updateTime", System.currentTimeMillis());
            
            redisTemplate.opsForValue().set(cacheKey, stats, PROBLEM_STATS_EXPIRE, TimeUnit.MINUTES);
            log.debug("题目统计信息已缓存：problemId={}", problemId);
        } catch (Exception e) {
            log.warn("缓存题目统计信息失败：problemId={}", problemId, e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProblemStats(Long problemId) {
        try {
            String cacheKey = PROBLEM_STATS_KEY + problemId;
            Map<String, Object> stats = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (stats != null) {
                return stats;
            }
        } catch (Exception e) {
            log.warn("获取题目统计信息缓存失败：problemId={}", problemId, e);
        }
        
        // 缓存未命中，从数据库获取并缓存
        Problem problem = problemMapper.selectByPrimaryKey(problemId);
        if (problem != null) {
            cacheProblemStats(problemId, problem.getSubmitCount(), problem.getAcceptCount());
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("submitCount", problem.getSubmitCount());
            stats.put("acceptCount", problem.getAcceptCount());
            stats.put("acceptRate", problem.getSubmitCount() > 0 ? 
                String.format("%.2f%%", 100.0 * problem.getAcceptCount() / problem.getSubmitCount()) : "0.00%");
            return stats;
        }
        
        return Collections.emptyMap();
    }
}
