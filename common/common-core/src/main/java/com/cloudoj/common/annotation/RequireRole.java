package com.cloudoj.common.annotation;

import java.lang.annotation.*;

/**
 * 需要角色注解
 * 标注在Controller方法上，表示该接口需要特定角色才能访问
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    
    /**
     * 允许的角色列表
     */
    String[] value();
}
