package com.example.mhproject.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cart_item")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long merchantId;
    private Long productId;
    private Integer quantity;
    private Boolean checked;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ================= 以下字段仅用于前端展示，不映射数据库列 =================
    @Transient
    private String productName;

    @Transient
    private String productImage;

    @Transient
    private BigDecimal productPrice;

    @Transient
    private BigDecimal totalPrice; // 单商品小计 = 单价 * 数量
}