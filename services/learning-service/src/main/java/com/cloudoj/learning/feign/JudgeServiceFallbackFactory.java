package com.cloudoj.learning.feign;

import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 评测服务降级处理
 */
@Slf4j
@Component
public class JudgeServiceFallbackFactory implements FallbackFactory<JudgeServiceClient> {
    
    @Override
    public JudgeServiceClient create(Throwable cause) {
        log.error("调用评测服务失败: {}", cause.getMessage());
        return new JudgeServiceClient() {
            @Override
            public Result<Map<String, Object>> getUserSubmissionStats(Long userId) {
                log.warn("获取用户提交统计降级处理: userId={}", userId);
                Map<String, Object> defaultStats = new HashMap<>();
                defaultStats.put("totalSubmissions", 0);
                defaultStats.put("totalAccepted", 0);
                return Result.success(defaultStats);
            }
        };
    }
}
