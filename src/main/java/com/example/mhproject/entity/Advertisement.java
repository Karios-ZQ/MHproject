package com.example.mhproject.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "advertisement")
public class Advertisement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description; // 新增：广告简短描述
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder; // 排序权重（越大越靠前）
    private Integer status;    // 0-禁用, 1-启用
    private String slot;       // 广告位：banner轮播 / popup弹窗 / zone专区
    private LocalDateTime startTime; // 排期开始（到点自动上线）
    private LocalDateTime endTime;   // 排期结束（到点自动下线）
    private Integer clickCount = 0;  // 点击次数
    private Integer viewCount = 0;   // 曝光次数
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}