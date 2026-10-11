package com.example.mhproject.config;

import java.lang.annotation.*;

/**
 * 接口限流注解：基于 Redis + Lua 实现分布式限流。
 * 用法：在 Controller 方法上加 @RateLimit(qps = 5) 表示每秒最多 5 次。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /** 每秒允许的请求数 */
    int qps() default 10;
}
