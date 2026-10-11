package com.example.mhproject.service;

import com.example.mhproject.entity.Order;
import com.example.mhproject.entity.OrderItem;
import com.example.mhproject.entity.Product;
import com.example.mhproject.repository.OrderItemRepository;
import com.example.mhproject.repository.OrderRepository;
import com.example.mhproject.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消：分布式定时任务。
 *
 * 每 1 分钟扫描一次"待付款"订单，
 * 超过 30 分钟未付款的自动取消，并同步归还 Redis 库存和数据库库存。
 *
 * 分布式多实例部署时，可用 ShedLock（Redis 分布式锁）保证只有一个实例执行，
 * 本项目为单实例演示，直接执行即可。
 */
@Service
public class OrderTimeoutService {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutService.class);
    private static final int TIMEOUT_MINUTES = 30;

    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private OrderItemRepository orderItemRepo;
    @Autowired
    private ProductRepository productRepo;
    @Autowired
    private RedisStockService stockService;

    /** 每分钟扫描一次超时未付款订单 */
    @Scheduled(fixedDelay = 60000)
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<Order> timeoutOrders = orderRepo.findByOrderStatusAndCreateTimeBefore(0, deadline);

        if (timeoutOrders.isEmpty()) return;

        log.info("===== 订单超时自动取消: 扫描到 {} 个超时未付款订单 =====", timeoutOrders.size());
        for (Order order : timeoutOrders) {
            try {
                // 1. 订单状态改为已取消(4)
                order.setOrderStatus(4);
                order.setUpdateTime(LocalDateTime.now());
                orderRepo.save(order);

                // 2. 归还 Redis 库存
                List<OrderItem> items = orderItemRepo.findByOrderId(order.getId());
                for (OrderItem item : items) {
                    stockService.restore(item.getProductId(), item.getQuantity());
                    // 3. 归还数据库库存
                    Product p = productRepo.findById(item.getProductId()).orElse(null);
                    if (p != null) {
                        p.setStock(p.getStock() + item.getQuantity());
                        productRepo.save(p);
                    }
                }
                log.info("订单 #{} 超时自动取消，库存已归还", order.getId());
            } catch (Exception e) {
                log.error("订单 #{} 超时取消失败", order.getId(), e);
            }
        }
    }
}
