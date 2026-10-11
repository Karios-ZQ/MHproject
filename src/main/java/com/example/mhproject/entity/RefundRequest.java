package com.example.mhproject.entity;
import jakarta.persistence.*; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @Entity @Table(name = "refund_request")
public class RefundRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long orderId; private Long userId; private Long merchantId;
    private BigDecimal refundAmount; private String refundReason;
    private Integer refundStatus; private LocalDateTime createTime; private LocalDateTime updateTime;
}