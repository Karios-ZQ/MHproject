package com.example.mhproject.repository;

import com.example.mhproject.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // 按订单ID查询明细（避免全表查询后在内存过滤）
    List<OrderItem> findByOrderId(Long orderId);
}
