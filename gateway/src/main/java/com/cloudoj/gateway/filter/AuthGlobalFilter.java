package com.cloudoj.gateway.filter;

import com.cloudoj.gateway.config.AuthProperties;
import com.cloudoj.gateway.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局认证过滤器
 * 验证JWT Token，将用户信息传递给下游服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    
    private final JwtUtil jwtUtil;
    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        String method = request.getMethod().name();
        
        log.debug("请求路径: {} {}", method, path);
        
        // 1. 检查是否在白名单中
        if (isWhitelisted(path)) {
            log.debug("白名单路径，放行: {}", path);
            return chain.filter(exchange);
        }
        
        // 2. 获取Token
        String token = getToken(request);
        if (token == null) {
            log.warn("缺少Token: {}", path);
            return unauthorized(exchange, "未登录，请先登录");
        }
        
        // 3. 验证Token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token无效或已过期: {}", path);
            return unauthorized(exchange, "登录已过期，请重新登录");
        }
        
        // 4. 解析用户信息
        Long userId;
        String username;
        String role;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
            username = jwtUtil.getUsernameFromToken(token);
            role = jwtUtil.getRoleFromToken(token);
        } catch (Exception e) {
            log.error("Token解析失败: {}", e.getMessage());
            return unauthorized(exchange, "Token无效");
        }
        
        log.debug("用户认证成功: userId={}, username={}, role={}", userId, username, role);
        
        // 5. 将用户信息添加到请求头，传递给下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-Username", username)
                .header("X-User-Role", role)
                .build();
        
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
    
    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        List<String> whitelist = authProperties.getWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        
        for (String pattern : whitelist) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 从请求中获取Token
     */
    private String getToken(ServerHttpRequest request) {
        // 优先从 Authorization 头获取
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 其次从查询参数获取
        String tokenParam = request.getQueryParams().getFirst("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }
        
        return null;
    }
    
    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", message);
        result.put("data", null);
        
        try {
            String json = objectMapper.writeValueAsString(result);
            DataBuffer buffer = response.bufferFactory()
                    .wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }
    
    @Override
    public int getOrder() {
        // 优先级较高，在路由之前执行
        return -100;
    }
}
