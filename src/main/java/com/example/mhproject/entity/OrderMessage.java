package com.example.mhproject.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 站内订单通知：下单确认、签收提示等系统消息。
 */
@Data
@Entity
@Table(name = "order_message")
public class OrderMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long orderId;
    private String type;      // create=下单确认, confirm=签收提示
    private String content;
    private Integer readFlag; // 0-未读, 1-已读
    private LocalDateTime createTime;
}
