package com.example.mhproject.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 基于 Redis + Lua 的分布式限流切面。
 * 多实例部署时，所有实例共享同一个 Redis 计数，实现全局限流。
 */
@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private StringRedisTemplate redis;

    private static final DefaultRedisScript<Long> SCRIPT;
    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setScriptText(
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local current = tonumber(redis.call('INCR', key)) " +
            "if current == 1 then redis.call('EXPIRE', key, 1) end " +
            "if current > limit then return 0 else return 1 end");
        SCRIPT.setResultType(Long.class);
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = "mh:ratelimit:" +
                pjp.getSignature().getDeclaringTypeName() + ":" +
                ((MethodSignature) pjp.getSignature()).getName();
        Long allowed = redis.execute(SCRIPT, Collections.singletonList(key),
                String.valueOf(rateLimit.qps()));
        if (allowed == null || allowed == 0L) {
            throw new RuntimeException("请求过于频繁，请稍后再试");
        }
        return pjp.proceed();
    }
}
