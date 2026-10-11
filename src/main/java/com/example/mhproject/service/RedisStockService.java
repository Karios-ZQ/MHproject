package com.example.mhproject.service;

import com.example.mhproject.entity.Product;
import com.example.mhproject.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Redis 库存服务：解决高并发下单"超卖"问题。
 *
 * 分布式原子扣减（Lua 脚本）：
 * 1. 应用启动时把数据库库存加载到 Redis（仅当 key 不存在）；
 * 2. 下单时用 Lua 脚本原子判断+扣减，从根上杜绝超卖；
 * 3. 数据库仍作为库存的唯一权威来源（source of truth）；
 * 4. 下单失败/事务回滚时调用 compensate() 把预扣库存加回；取消订单时调用 restore() 归还。
 */
@Service
public class RedisStockService {

    private static final Logger log = LoggerFactory.getLogger(RedisStockService.class);
    private static final String STOCK_PREFIX = "mh:stock:";

    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private ProductRepository productRepo;

    /** Lua 原子扣减：库存充足才扣减，否则返回 -1 不产生负库存 */
    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT;
    static {
        DEDUCT_SCRIPT = new DefaultRedisScript<>();
        DEDUCT_SCRIPT.setScriptText(
            "local key = KEYS[1] " +
            "local qty = tonumber(ARGV[1]) " +
            "local stock = tonumber(redis.call('GET', key)) " +
            "if stock == nil then return -2 end " +
            "if stock < qty then return -1 end " +
            "return redis.call('DECRBY', key, qty)");
        DEDUCT_SCRIPT.setResultType(Long.class);
    }

    /** 应用就绪后，把数据库库存预热到 Redis（仅当 key 不存在） */
    @EventListener(ApplicationReadyEvent.class)
    public void initAllStocks() {
        List<Product> products = productRepo.findAll();
        int set = 0;
        for (Product p : products) {
            if (p.getStock() != null) {
                boolean ok = Boolean.TRUE.equals(redis.opsForValue()
                        .setIfAbsent(STOCK_PREFIX + p.getId(), String.valueOf(p.getStock())));
                if (ok) set++;
            }
        }
        log.info("==== RedisStockService 库存预热完成: 商品{}个, 新写入key {}个 ====", products.size(), set);
    }

    /**
     * 原子预扣库存（Lua 脚本，单次往返）。
     * @return true=扣减成功；false=库存不足或商品不存在
     */
    public boolean preDeduct(Long productId, int qty) {
        if (productId == null || qty <= 0) return false;
        String key = STOCK_PREFIX + productId;
        ensureKey(key, productId);
        Long remain = redis.execute(DEDUCT_SCRIPT, Collections.singletonList(key), String.valueOf(qty));
        // remain < 0 表示库存不足，脚本未做任何修改
        return remain != null && remain >= 0;
    }

    /** 补偿：下单失败 / 事务回滚时，把预扣的库存加回去 */
    public void compensate(Long productId, int qty) {
        if (productId == null || qty <= 0) return;
        redis.opsForValue().increment(STOCK_PREFIX + productId, qty);
    }

    /** 恢复：取消订单时归还库存（与数据库同步） */
    public void restore(Long productId, int qty) {
        compensate(productId, qty);
    }

    /** 查询 Redis 中的实时可用库存（无 key 时回退数据库库存） */
    public long availableStock(Long productId) {
        String key = STOCK_PREFIX + productId;
        ensureKey(key, productId);
        String v = redis.opsForValue().get(key);
        try {
            return v == null ? 0L : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void ensureKey(String key, Long productId) {
        if (Boolean.FALSE.equals(redis.hasKey(key))) {
            Product p = productRepo.findById(productId).orElse(null);
            int stock = (p != null && p.getStock() != null) ? p.getStock() : 0;
            redis.opsForValue().setIfAbsent(key, String.valueOf(stock));
        }
    }
}
