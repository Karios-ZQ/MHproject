package com.example.mhproject.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long merchantId;
    private String name;
    private String category; // 商品分类：如 文创/文具/家居/数码/服饰
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    private String description; // 商品详情描述
    private String imageUrl; // 👈 新增：商品图片URL
    private LocalDateTime createTime;
}