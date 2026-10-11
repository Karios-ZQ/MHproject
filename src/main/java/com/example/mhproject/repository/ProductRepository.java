package com.example.mhproject.repository;
import com.example.mhproject.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 按分类查询
    List<Product> findByCategory(String category);

    // 关键词模糊搜索（商品名）
    List<Product> findByNameContaining(String keyword);

    // 按价格升序
    List<Product> findAllByOrderByPriceAsc();

    // 按价格降序
    List<Product> findAllByOrderByPriceDesc();

    // 按创建时间倒序（最新）
    List<Product> findAllByOrderByCreateTimeDesc();

    // 关键词 + 分类组合搜索
    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:keyword IS NULL OR :keyword = '' OR p.name LIKE CONCAT('%', :keyword, '%')) " +
            "ORDER BY p.createTime DESC")
    List<Product> search(@Param("category") String category, @Param("keyword") String keyword);
}
