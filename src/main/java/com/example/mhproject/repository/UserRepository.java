package com.example.mhproject.repository;

import com.example.mhproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // 根据用户名查询用户（用于登录）
    User findByUsername(String username);
}