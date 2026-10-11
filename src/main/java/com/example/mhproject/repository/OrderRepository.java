package com.example.mhproject.repository;
import com.example.mhproject.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 根据用户ID查询订单，按创建时间倒序
    List<Order> findByUserIdOrderByCreateTimeDesc(Long userId);

    // 全部订单按创建时间倒序（管理员用）
    List<Order> findAllByOrderByCreateTimeDesc();

    // 超时未付款订单（定时任务用）
    List<Order> findByOrderStatusAndCreateTimeBefore(Integer orderStatus, LocalDateTime createTimeBefore);
}
