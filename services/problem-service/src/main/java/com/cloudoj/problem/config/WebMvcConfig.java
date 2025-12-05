package com.cloudoj.problem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    /**
     * 配置路径匹配
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 不使用后缀模式匹配
        configurer.setUseSuffixPatternMatch(false);
        // 不使用尾部斜杠匹配
        configurer.setUseTrailingSlashMatch(false);
    }
    
    /**
     * 配置静态资源处理器
     * 设置较低的优先级，确保 Controller 优先匹配
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 设置静态资源处理器的优先级最低
        registry.setOrder(Integer.MAX_VALUE);
    }
}
