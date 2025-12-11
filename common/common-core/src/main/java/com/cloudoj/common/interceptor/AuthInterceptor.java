package com.cloudoj.common.interceptor;

import com.cloudoj.common.annotation.RequireLogin;
import com.cloudoj.common.annotation.RequireRole;
import com.cloudoj.common.context.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 权限校验拦截器
 * 处理 @RequireLogin 和 @RequireRole 注解
 */
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 检查 @RequireLogin 注解
        RequireLogin requireLogin = handlerMethod.getMethodAnnotation(RequireLogin.class);
        if (requireLogin == null) {
            requireLogin = handlerMethod.getBeanType().getAnnotation(RequireLogin.class);
        }
        
        if (requireLogin != null) {
            if (UserContext.getUserId() == null) {
                sendError(response, 401, "请先登录");
                return false;
            }
        }
        
        // 检查 @RequireRole 注解
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        
        if (requireRole != null) {
            if (UserContext.getUserId() == null) {
                sendError(response, 401, "请先登录");
                return false;
            }
            
            String currentRole = UserContext.getRole();
            String[] allowedRoles = requireRole.value();
            
            boolean hasRole = Arrays.stream(allowedRoles)
                    .anyMatch(role -> role.equals(currentRole));
            
            if (!hasRole) {
                log.warn("权限不足: userId={}, role={}, required={}", 
                        UserContext.getUserId(), currentRole, Arrays.toString(allowedRoles));
                sendError(response, 403, "权限不足");
                return false;
            }
        }
        
        return true;
    }
    
    private void sendError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);
        
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
