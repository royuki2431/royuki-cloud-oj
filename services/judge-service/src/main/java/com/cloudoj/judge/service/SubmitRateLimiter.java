package com.cloudoj.judge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 提交频率限制服务
 * 使用Redis实现分布式限流
 */
@Slf4j
@Service
public class SubmitRateLimiter {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Redis key前缀
    private static final String RATE_LIMIT_PREFIX = "submit:rate:";
    
    // 限制规则：每个用户每分钟最多提交5次
    private static final int MAX_SUBMITS_PER_MINUTE = 5;
    private static final long TIME_WINDOW_SECONDS = 60;
    
    // 限制规则：每个用户每小时最多提交30次
    private static final int MAX_SUBMITS_PER_HOUR = 30;
    private static final long HOUR_WINDOW_SECONDS = 3600;
    
    /**
     * 检查用户是否可以提交
     * 
     * @param userId 用户ID
     * @return true-可以提交，false-超出限制
     */
    public boolean canSubmit(Long userId) {
        // 检查1分钟内的提交次数
        if (!checkRateLimit(userId, "minute", MAX_SUBMITS_PER_MINUTE, TIME_WINDOW_SECONDS)) {
            log.warn("用户提交频率超限(1分钟), userId={}", userId);
            return false;
        }
        
        // 检查1小时内的提交次数
        if (!checkRateLimit(userId, "hour", MAX_SUBMITS_PER_HOUR, HOUR_WINDOW_SECONDS)) {
            log.warn("用户提交频率超限(1小时), userId={}", userId);
            return false;
        }
        
        return true;
    }
    
    /**
     * 记录一次提交
     * 
     * @param userId 用户ID
     */
    public void recordSubmit(Long userId) {
        // 记录1分钟窗口
        incrementCounter(userId, "minute", TIME_WINDOW_SECONDS);
        
        // 记录1小时窗口
        incrementCounter(userId, "hour", HOUR_WINDOW_SECONDS);
        
        log.debug("记录用户提交, userId={}", userId);
    }
    
    /**
     * 获取剩余可提交次数
     * 
     * @param userId 用户ID
     * @return 剩余次数
     */
    public int getRemainingSubmits(Long userId) {
        String keyMinute = RATE_LIMIT_PREFIX + userId + ":minute";
        Integer countMinute = (Integer) redisTemplate.opsForValue().get(keyMinute);
        
        if (countMinute == null) {
            return MAX_SUBMITS_PER_MINUTE;
        }
        
        return Math.max(0, MAX_SUBMITS_PER_MINUTE - countMinute);
    }
    
    /**
     * 获取需要等待的秒数
     * 
     * @param userId 用户ID
     * @return 等待秒数
     */
    public long getWaitSeconds(Long userId) {
        String keyMinute = RATE_LIMIT_PREFIX + userId + ":minute";
        Long ttl = redisTemplate.getExpire(keyMinute, TimeUnit.SECONDS);
        
        if (ttl == null || ttl < 0) {
            return 0;
        }
        
        return ttl;
    }
    
    /**
     * 检查频率限制
     * 
     * @param userId 用户ID
     * @param window 时间窗口标识
     * @param maxCount 最大次数
     * @param windowSeconds 窗口秒数
     * @return true-未超限，false-已超限
     */
    private boolean checkRateLimit(Long userId, String window, int maxCount, long windowSeconds) {
        String key = RATE_LIMIT_PREFIX + userId + ":" + window;
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        
        if (count == null) {
            return true;
        }
        
        return count < maxCount;
    }
    
    /**
     * 增加计数器
     * 
     * @param userId 用户ID
     * @param window 时间窗口标识
     * @param windowSeconds 窗口秒数
     */
    private void incrementCounter(Long userId, String window, long windowSeconds) {
        String key = RATE_LIMIT_PREFIX + userId + ":" + window;
        
        Long count = redisTemplate.opsForValue().increment(key);
        
        // 首次设置过期时间
        if (count != null && count == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 清除用户的限流记录（用于测试或管理员操作）
     * 
     * @param userId 用户ID
     */
    public void clearRateLimit(Long userId) {
        String keyMinute = RATE_LIMIT_PREFIX + userId + ":minute";
        String keyHour = RATE_LIMIT_PREFIX + userId + ":hour";
        
        redisTemplate.delete(keyMinute);
        redisTemplate.delete(keyHour);
        
        log.info("已清除用户限流记录, userId={}", userId);
    }
}
