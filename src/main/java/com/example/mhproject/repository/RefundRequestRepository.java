package com.example.mhproject.repository;
import com.example.mhproject.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    // 查询用户的退款记录
    List<RefundRequest> findByUserId(Long userId);

    // 全部退款申请，按创建时间倒序（管理员用）
    List<RefundRequest> findAllByOrderByCreateTimeDesc();
}
