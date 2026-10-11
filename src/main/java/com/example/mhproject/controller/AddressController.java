package com.example.mhproject.controller;

import com.example.mhproject.entity.UserAddress;
import com.example.mhproject.repository.UserAddressRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/address")
@CrossOrigin
public class AddressController {

    @Autowired
    private UserAddressRepository addressRepo;

    private Long getLoginUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) throw new RuntimeException("未登录，请先登录账号！");
        return userId;
    }

    // 获取用户的地址列表
    @GetMapping("/list")
    public List<UserAddress> getAddressList(HttpSession session) {
        return addressRepo.findByUserIdAndIsDeletedFalseOrderByIsDefaultDesc(getLoginUserId(session));
    }

    // 新增地址
    @PostMapping("/add")
    public UserAddress addAddress(@RequestBody UserAddress address, HttpSession session) {
        Long userId = getLoginUserId(session);
        address.setUserId(userId);
        address.setIsDeleted(false);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());

        // 如果这是用户的第一个地址，或者设置了为默认地址，则清除其他默认地址
        List<UserAddress> existing = addressRepo.findByUserIdAndIsDeletedFalseOrderByIsDefaultDesc(userId);
        if (existing.isEmpty()) {
            address.setIsDefault(true);
        } else if (Boolean.TRUE.equals(address.getIsDefault())) {
            for (UserAddress addr : existing) {
                addr.setIsDefault(false);
                addressRepo.save(addr);
            }
        }
        return addressRepo.save(address);
    }

    // 设置默认地址
    @PostMapping("/setDefault")
    public String setDefault(@RequestParam Long addressId, HttpSession session) {
        Long userId = getLoginUserId(session);
        List<UserAddress> existing = addressRepo.findByUserIdAndIsDeletedFalseOrderByIsDefaultDesc(userId);
        for (UserAddress addr : existing) {
            addr.setIsDefault(addr.getId().equals(addressId));
            addressRepo.save(addr);
        }
        return "设置成功";
    }

    // 删除地址（逻辑删除）
    @PostMapping("/delete")
    public String deleteAddress(@RequestParam Long addressId, HttpSession session) {
        getLoginUserId(session);
        UserAddress addr = addressRepo.findById(addressId).orElseThrow(() -> new RuntimeException("地址不存在"));
        addr.setIsDeleted(true);
        addressRepo.save(addr);
        return "删除成功";
    }
}