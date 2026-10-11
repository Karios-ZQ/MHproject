package com.example.mhproject.service;

import com.example.mhproject.entity.Product;
import com.example.mhproject.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 布隆过滤器：防缓存穿透。
 *
 * 恶意请求大量查询不存在的商品 ID（如 -1、999999），
 * 每次都打到数据库，把数据库压垮。
 *
 * 方案：用 Redis BITMAP 实现布隆过滤器，
 * 查询前先过过滤器，"肯定不存在"的直接拒绝，不查数据库。
 */
@Service
public class BloomFilterService {

    private static final Logger log = LoggerFactory.getLogger(BloomFilterService.class);
    private static final String BITMAP_KEY = "mh:bloom:product";
    private static final int BIT_SIZE = 1 << 20; // 约 100 万位
    private static final int[] SEEDS = {3, 7, 11, 13, 17}; // 5 个哈希函数

    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private ProductRepository productRepo;

    /** 启动时把所有商品 ID 加入布隆过滤器 */
    @EventListener(ApplicationReadyEvent.class)
    public void initBloomFilter() {
        List<Product> products = productRepo.findAll();
        for (Product p : products) {
            add(p.getId());
        }
        log.info("===== 布隆过滤器初始化完成: 商品 {} 个 =====", products.size());
    }

    /** 新增商品时加入布隆过滤器 */
    public void add(Long productId) {
        if (productId == null) return;
        for (int seed : SEEDS) {
            int offset = hash(productId, seed) % BIT_SIZE;
            redis.opsForValue().setBit(BITMAP_KEY, offset, true);
        }
    }

    /**
     * 判断商品 ID 是否可能存在。
     * @return true=可能存在（需要查数据库）；false=一定不存在（直接拒绝）
     */
    public boolean mightExist(Long productId) {
        if (productId == null || productId <= 0) return false;
        for (int seed : SEEDS) {
            int offset = hash(productId, seed) % BIT_SIZE;
            Boolean bit = redis.opsForValue().getBit(BITMAP_KEY, offset);
            if (bit == null || !bit) return false;
        }
        return true;
    }

    /** 简单的哈希函数：productId + seed 混合 */
    private int hash(Long productId, int seed) {
        long h = productId * 31L + seed * 17L;
        return (int) Math.abs(h % BIT_SIZE);
    }
}
