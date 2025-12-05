package com.cloudoj.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/user")
public class HealthController {
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "user-service");
        result.put("time", LocalDateTime.now());
        result.put("message", "User Service is running!");
        return result;
    }
    
    @GetMapping("/test")
    public String test() {
        return "User Service Test OK!";
    }
}
