package com.example.mhproject.service;

import com.example.mhproject.entity.OrderMessage;
import com.example.mhproject.repository.OrderMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内订单通知服务：下单确认、签收提示等系统消息的生成与查询。
 */
@Service
public class NotificationService {

    @Autowired
    private OrderMessageRepository msgRepo;

    @Transactional
    public void notifyUser(Long userId, Long orderId, String type, String content) {
        OrderMessage m = new OrderMessage();
        m.setUserId(userId);
        m.setOrderId(orderId);
        m.setType(type);
        m.setContent(content);
        m.setReadFlag(0);
        m.setCreateTime(LocalDateTime.now());
        msgRepo.save(m);
    }

    public List<OrderMessage> getUserMessages(Long userId) {
        return msgRepo.findByUserIdOrderByCreateTimeDesc(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<OrderMessage> list = msgRepo.findByUserIdOrderByCreateTimeDesc(userId);
        for (OrderMessage m : list) {
            if (m.getReadFlag() == null || m.getReadFlag() == 0) {
                m.setReadFlag(1);
            }
        }
        msgRepo.saveAll(list);
    }
}
