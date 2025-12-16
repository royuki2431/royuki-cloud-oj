package com.cloudoj.learning.feign;

import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 题目服务降级工厂
 */
@Slf4j
@Component
public class ProblemServiceFallbackFactory implements FallbackFactory<ProblemServiceClient> {
    
    @Override
    public ProblemServiceClient create(Throwable cause) {
        log.error("题目服务调用失败", cause);
        return new ProblemServiceClient() {
            @Override
            public Result<Map<String, Object>> getProblemById(Long id) {
                log.warn("获取题目信息降级处理, id={}", id);
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("id", id);
                fallback.put("title", "题目 #" + id);
                return Result.success(fallback);
            }
        };
    }
}
