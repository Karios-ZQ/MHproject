package com.example.mhproject.repository;
import com.example.mhproject.entity.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    // 查询状态为1(启用)的广告，按权重降序
    List<Advertisement> findByStatusOrderBySortOrderDesc(Integer status);
}