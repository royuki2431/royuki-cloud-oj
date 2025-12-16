package com.cloudoj.learning.feign;

import com.cloudoj.model.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 评测服务 Feign 客户端
 */
@FeignClient(name = "judge-service", fallbackFactory = JudgeServiceFallbackFactory.class)
public interface JudgeServiceClient {
    
    /**
     * 获取用户提交统计
     */
    @GetMapping("/judge/statistics/user/{userId}")
    Result<Map<String, Object>> getUserSubmissionStats(@PathVariable("userId") Long userId);
}
