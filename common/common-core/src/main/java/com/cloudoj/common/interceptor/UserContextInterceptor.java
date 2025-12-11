package com.cloudoj.common.interceptor;

import com.cloudoj.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器
 * 从请求头中提取Gateway传递的用户信息，存入ThreadLocal
 */
@Slf4j
public class UserContextInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头获取用户信息（由Gateway传递）
        String userIdStr = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        String role = request.getHeader("X-User-Role");
        
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdStr);
                UserContext.setUserId(userId);
                UserContext.setUsername(username);
                UserContext.setRole(role);
                log.debug("用户上下文设置成功: userId={}, username={}, role={}", userId, username, role);
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID: {}", userIdStr);
            }
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        // 请求结束，清除上下文
        UserContext.clear();
    }
}
