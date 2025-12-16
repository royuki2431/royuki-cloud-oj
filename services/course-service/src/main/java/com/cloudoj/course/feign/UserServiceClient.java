package com.cloudoj.course.feign;

import com.cloudoj.model.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 用户服务 Feign 客户端
 */
@FeignClient(name = "user-service", fallbackFactory = UserServiceFallbackFactory.class)
public interface UserServiceClient {
    
    /**
     * 获取用户信息
     */
    @GetMapping("/user/info/{userId}")
    Result<Map<String, Object>> getUserInfo(@PathVariable("userId") Long userId);
}
