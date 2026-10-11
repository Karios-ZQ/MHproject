package com.example.mhproject.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer orderStatus; // 0-待付款, 1-已付款, 2-已发货, 3-已完成, 4-已取消, 5-退款中, 6-已退款, 7-退款驳回
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 仅用于前端展示订单明细，不参与JPA映射
    @Transient
    private List<OrderItem> items;
}