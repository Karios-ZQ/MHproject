package com.example.mhproject.controller;

import com.example.mhproject.entity.*;
import com.example.mhproject.config.RateLimit;
import com.example.mhproject.service.MallService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class MallController {

    @Autowired
    private MallService mallService;

    // 核心安全拦截方法：未登录直接抛出异常
    private Long getLoginUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("未登录，请先登录账号！");
        }
        return userId;
    }

    // ================= 游客可访问的接口 =================

    // 商品列表：支持分类 category、关键词 keyword、排序 sort(price_asc/price_desc/newest)
    @GetMapping("/products")
    public List<Product> getProducts(@RequestParam(required = false) String category,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String sort) {
        return mallService.getProducts(category, keyword, sort);
    }

    // 商品详情
    @GetMapping("/product/{id}")
    public Product getProductDetail(@PathVariable Long id) {
        return mallService.getProductDetail(id);
    }

    // 商品分类列表
    @GetMapping("/categories")
    public List<String> getCategories() {
        return mallService.getCategories();
    }

    @GetMapping("/ads")
    public List<Advertisement> getAds() {
        return mallService.getActiveAds();
    }

    // ================= 购物车模块（需登录） =================

    @PostMapping("/cart/add")
    public String addCart(@RequestParam Long productId, @RequestParam Integer quantity, HttpSession session) {
        Long userId = getLoginUserId(session);
        mallService.addToCart(userId, productId, quantity);
        return "添加成功";
    }

    @GetMapping("/cart/list")
    public List<CartItem> getCart(HttpSession session) {
        return mallService.getCart(getLoginUserId(session));
    }

    @PostMapping("/cart/updateChecked")
    public String updateChecked(@RequestParam Long cartId, @RequestParam Boolean checked, HttpSession session) {
        getLoginUserId(session);
        mallService.updateCartChecked(cartId, checked);
        return "更新成功";
    }

    @PostMapping("/cart/updateQuantity")
    public String updateQuantity(@RequestParam Long cartId, @RequestParam Integer quantity, HttpSession session) {
        getLoginUserId(session);
        mallService.updateCartQuantity(cartId, quantity);
        return "更新成功";
    }

    @PostMapping("/cart/delete")
    public String deleteCart(@RequestParam Long cartId, HttpSession session) {
        getLoginUserId(session);
        mallService.deleteCartItem(cartId);
        return "删除成功";
    }

    // ================= 订单模块（需登录） =================

    @PostMapping("/order/create")
    @RateLimit(qps = 10)
    public Order createOrder(@RequestParam Long addressId, HttpSession session) {
        Long userId = getLoginUserId(session);
        return mallService.createOrder(userId, addressId);
    }

    @PostMapping("/order/pay")
    public String payOrder(@RequestParam Long orderId, HttpSession session) {
        getLoginUserId(session);
        mallService.payOrder(orderId);
        return "支付成功";
    }

    @PostMapping("/order/cancel")
    public String cancelOrder(@RequestParam Long orderId, HttpSession session) {
        getLoginUserId(session);
        mallService.cancelOrder(orderId);
        return "已取消";
    }

    @GetMapping("/order/list")
    public List<Order> getOrders(HttpSession session) {
        return mallService.getUserOrders(getLoginUserId(session));
    }

    // 确认收货（已发货 -> 已完成）
    @PostMapping("/order/confirm")
    public String confirmReceipt(@RequestParam Long orderId, HttpSession session) {
        Long userId = getLoginUserId(session);
        mallService.confirmReceipt(userId, orderId);
        return "已确认收货";
    }

    // ================= 售后退款模块（需登录） =================

    @PostMapping("/refund/apply")
    public String applyRefund(@RequestParam Long orderId, @RequestParam String reason, HttpSession session) {
        getLoginUserId(session);
        mallService.applyRefund(orderId, reason);
        return "退款申请已提交";
    }
}