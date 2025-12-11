package com.cloudoj.common.annotation;

import java.lang.annotation.*;

/**
 * 需要登录注解
 * 标注在Controller方法上，表示该接口需要登录才能访问
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireLogin {
}
