package com.cloudoj.learning.feign;

import com.cloudoj.model.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 题目服务 Feign 客户端
 */
@FeignClient(name = "problem-service", fallbackFactory = ProblemServiceFallbackFactory.class)
public interface ProblemServiceClient {
    
    /**
     * 获取题目信息
     */
    @GetMapping("/problem/{id}")
    Result<Map<String, Object>> getProblemById(@PathVariable("id") Long id);
}
