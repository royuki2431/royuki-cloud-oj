package com.cloudoj.common.config;

import com.cloudoj.common.interceptor.AuthInterceptor;
import com.cloudoj.common.interceptor.UserContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 公共 Web MVC 配置
 * 注册用户上下文拦截器和权限拦截器
 */
@Configuration
public class CommonWebMvcConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 用户上下文拦截器（优先级最高）
        registry.addInterceptor(new UserContextInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/error")
                .order(0);
        
        // 2. 权限校验拦截器
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/error")
                .order(1);
    }
}
