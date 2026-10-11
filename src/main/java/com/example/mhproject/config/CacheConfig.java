package com.example.mhproject.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存：热点商品详情的 L1 缓存。
 * 配合 Redis（L2）实现二级缓存架构，降低数据库读压力。
 */
@Configuration
public class CacheConfig {

    /** 商品详情本地缓存：10 分钟过期，最大 500 条 */
    @Bean("productDetailCache")
    public Cache<Long, Object> productDetailCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build();
    }
}
