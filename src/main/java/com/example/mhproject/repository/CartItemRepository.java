package com.example.mhproject.repository;
import com.example.mhproject.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // 查找用户已勾选的购物车项
    List<CartItem> findByUserIdAndCheckedTrue(Long userId);
    // 查找用户所有购物车项
    List<CartItem> findByUserId(Long userId);
}