package com.example.mhproject.repository;

import com.example.mhproject.entity.OrderMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderMessageRepository extends JpaRepository<OrderMessage, Long> {
    List<OrderMessage> findByUserIdOrderByCreateTimeDesc(Long userId);
}
