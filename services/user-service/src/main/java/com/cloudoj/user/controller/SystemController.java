package com.cloudoj.user.controller;

import com.cloudoj.common.annotation.RequireRole;
import com.cloudoj.model.common.Result;
import com.cloudoj.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/user/system")
public class SystemController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Value("${spring.application.name:user-service}")
    private String applicationName;
    
    // 系统启动时间
    private static final LocalDateTime START_TIME = LocalDateTime.now();
    
    // 系统设置缓存
    private static final Map<String, Object> SETTINGS_CACHE = new ConcurrentHashMap<>();
    
    // 初始化默认设置
    static {
        // 基础设置
        SETTINGS_CACHE.put("systemName", "Royuki Cloud OJ");
        SETTINGS_CACHE.put("systemDesc", "在线代码评测系统");
        SETTINGS_CACHE.put("keywords", "OJ,编程,算法,数据结构");
        SETTINGS_CACHE.put("icp", "");
        SETTINGS_CACHE.put("allowRegister", true);
        SETTINGS_CACHE.put("requireEmailVerify", false);
        SETTINGS_CACHE.put("minPasswordLength", 6);
        SETTINGS_CACHE.put("maxLoginAttempts", 5);
        
        // 评测设置
        SETTINGS_CACHE.put("defaultTimeLimit", 1000);
        SETTINGS_CACHE.put("defaultMemoryLimit", 256);
        SETTINGS_CACHE.put("maxConcurrentJudge", 10);
        SETTINGS_CACHE.put("judgeTimeout", 60);
        SETTINGS_CACHE.put("supportedLanguages", "Java,Python,C++,C");
        SETTINGS_CACHE.put("submitInterval", 5);
    }
    
    /**
     * 获取系统设置
     */
    @GetMapping("/settings")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public Result<Map<String, Object>> getSettings() {
        log.info("获取系统设置");
        
        // 从Redis获取设置，如果没有则使用默认值
        Map<String, Object> settings = new HashMap<>();
        for (String key : SETTINGS_CACHE.keySet()) {
            String redisKey = "system:settings:" + key;
            String value = redisTemplate.opsForValue().get(redisKey);
            if (value != null) {
                settings.put(key, parseValue(key, value));
            } else {
                settings.put(key, SETTINGS_CACHE.get(key));
            }
        }
        
        return Result.success(settings);
    }
    
    /**
     * 更新系统设置
     */
    @PostMapping("/settings")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public Result<Void> updateSettings(@RequestBody Map<String, Object> settings) {
        log.info("更新系统设置：{}", settings.keySet());
        
        // 保存到Redis
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String redisKey = "system:settings:" + entry.getKey();
            String value = String.valueOf(entry.getValue());
            redisTemplate.opsForValue().set(redisKey, value);
            // 同时更新缓存
            SETTINGS_CACHE.put(entry.getKey(), entry.getValue());
        }
        
        return Result.success("设置保存成功", null);
    }
    
    /**
     * 获取系统信息
     */
    @GetMapping("/info")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public Result<Map<String, Object>> getSystemInfo() {
        log.info("获取系统信息");
        
        Map<String, Object> info = new HashMap<>();
        
        // 系统版本和启动时间
        info.put("version", "v1.0.0");
        info.put("startTime", START_TIME.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.put("uptime", formatUptime(Duration.between(START_TIME, LocalDateTime.now())));
        
        // 服务状态
        info.put("dbStatus", checkDatabaseStatus());
        info.put("redisStatus", checkRedisStatus());
        info.put("mqStatus", true); // 简化处理
        
        // 统计数据
        info.put("userCount", userService.countUsers(null, null, null));
        
        // 系统资源
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        // CPU 使用率 - 使用 com.sun.management 获取更准确的值
        int cpuUsage = 0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            double cpuLoad = sunOsBean.getCpuLoad(); // 返回 0.0 到 1.0
            if (cpuLoad >= 0) {
                cpuUsage = (int) (cpuLoad * 100);
            } else {
                // 如果获取失败，使用进程 CPU 使用率
                double processCpuLoad = sunOsBean.getProcessCpuLoad();
                cpuUsage = processCpuLoad >= 0 ? (int) (processCpuLoad * 100) : 25;
            }
        } else {
            // 降级：使用系统负载平均值估算
            double loadAvg = osBean.getSystemLoadAverage();
            int processors = osBean.getAvailableProcessors();
            cpuUsage = loadAvg >= 0 ? (int) Math.min(100, (loadAvg / processors) * 100) : 25;
        }
        info.put("cpuUsage", cpuUsage);
        
        // 内存使用率
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        info.put("memoryUsage", maxMemory > 0 ? (int) (usedMemory * 100 / maxMemory) : 0);
        
        // 磁盘使用率
        java.io.File root = new java.io.File("/");
        if (root.exists()) {
            long totalSpace = root.getTotalSpace();
            long usableSpace = root.getUsableSpace();
            info.put("diskUsage", totalSpace > 0 ? (int) ((totalSpace - usableSpace) * 100 / totalSpace) : 0);
        } else {
            // Windows 使用 C 盘
            java.io.File cDrive = new java.io.File("C:");
            if (cDrive.exists()) {
                long totalSpace = cDrive.getTotalSpace();
                long usableSpace = cDrive.getUsableSpace();
                info.put("diskUsage", totalSpace > 0 ? (int) ((totalSpace - usableSpace) * 100 / totalSpace) : 0);
            } else {
                info.put("diskUsage", 0);
            }
        }
        
        return Result.success(info);
    }
    
    /**
     * 获取缓存信息
     */
    @GetMapping("/cache/info")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public Result<Map<String, Object>> getCacheInfo() {
        log.info("获取缓存信息");
        
        Map<String, Object> cacheInfo = new HashMap<>();
        
        try {
            // 获取Redis键数量
            Set<String> keys = redisTemplate.keys("*");
            cacheInfo.put("redisKeys", keys != null ? keys.size() : 0);
            
            // 获取Session数量
            Set<String> sessionKeys = redisTemplate.keys("session:*");
            cacheInfo.put("sessionCount", sessionKeys != null ? sessionKeys.size() : 0);
            
            // 获取各类缓存数量
            Set<String> problemKeys = redisTemplate.keys("problem:*");
            cacheInfo.put("problemCacheCount", problemKeys != null ? problemKeys.size() : 0);
            
            Set<String> userKeys = redisTemplate.keys("user:*");
            cacheInfo.put("userCacheCount", userKeys != null ? userKeys.size() : 0);
        } catch (Exception e) {
            log.error("获取缓存信息失败", e);
            cacheInfo.put("redisKeys", 0);
            cacheInfo.put("sessionCount", 0);
        }
        
        return Result.success(cacheInfo);
    }
    
    /**
     * 清除缓存
     */
    @PostMapping("/cache/clear")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public Result<Void> clearCache(@RequestParam String type) {
        log.info("清除缓存：type={}", type);
        
        try {
            switch (type) {
                case "redis":
                    // 清除所有缓存（保留系统设置）
                    Set<String> keys = redisTemplate.keys("*");
                    if (keys != null) {
                        keys.stream()
                            .filter(key -> !key.startsWith("system:settings:"))
                            .forEach(redisTemplate::delete);
                    }
                    break;
                case "session":
                    // 清除所有Session
                    Set<String> sessionKeys = redisTemplate.keys("session:*");
                    if (sessionKeys != null) {
                        sessionKeys.forEach(redisTemplate::delete);
                    }
                    break;
                case "problem":
                    // 清除题目缓存
                    Set<String> problemKeys = redisTemplate.keys("problem:*");
                    if (problemKeys != null) {
                        problemKeys.forEach(redisTemplate::delete);
                    }
                    break;
                default:
                    return Result.error("未知的缓存类型");
            }
            
            return Result.success("缓存清除成功", null);
        } catch (Exception e) {
            log.error("清除缓存失败", e);
            return Result.error("清除缓存失败：" + e.getMessage());
        }
    }
    
    /**
     * 检查数据库状态
     */
    private boolean checkDatabaseStatus() {
        try {
            userService.countUsers(null, null, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查Redis状态
     */
    private boolean checkRedisStatus() {
        try {
            redisTemplate.opsForValue().get("health:check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 格式化运行时长
     */
    private String formatUptime(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天 ");
        }
        sb.append(hours).append("小时 ");
        sb.append(minutes).append("分钟");
        
        return sb.toString();
    }
    
    /**
     * 解析设置值
     */
    private Object parseValue(String key, String value) {
        // 布尔类型
        if (key.equals("allowRegister") || key.equals("requireEmailVerify") || key.equals("smtpSsl")) {
            return Boolean.parseBoolean(value);
        }
        // 数字类型
        if (key.endsWith("Length") || key.endsWith("Limit") || key.endsWith("Attempts") 
            || key.endsWith("Judge") || key.endsWith("Timeout") || key.endsWith("Interval")
            || key.endsWith("Port")) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return SETTINGS_CACHE.get(key);
            }
        }
        return value;
    }
}
