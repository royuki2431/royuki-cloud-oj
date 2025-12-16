package com.cloudoj.course.feign;

import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务降级工厂
 */
@Slf4j
@Component
public class UserServiceFallbackFactory implements FallbackFactory<UserServiceClient> {
    
    @Override
    public UserServiceClient create(Throwable cause) {
        log.error("用户服务调用失败", cause);
        return new UserServiceClient() {
            @Override
            public Result<Map<String, Object>> getUserInfo(Long userId) {
                log.warn("获取用户信息降级处理, userId={}", userId);
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("id", userId);
                fallback.put("username", "用户" + userId);
                fallback.put("nickname", "未知用户");
                return Result.success(fallback);
            }
        };
    }
}
