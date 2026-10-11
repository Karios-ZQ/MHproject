package com.example.mhproject.repository;

import com.example.mhproject.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    // 查询用户的所有有效地址（按是否默认倒序）
    List<UserAddress> findByUserIdAndIsDeletedFalseOrderByIsDefaultDesc(Long userId);
}