package com.example.mhproject.controller;

import com.example.mhproject.entity.Advertisement;
import com.example.mhproject.entity.Order;
import com.example.mhproject.entity.Product;
import com.example.mhproject.entity.RefundRequest;
import com.example.mhproject.entity.User;
import com.example.mhproject.repository.AdvertisementRepository;
import com.example.mhproject.repository.ProductRepository;
import com.example.mhproject.repository.UserRepository;
import com.example.mhproject.service.BloomFilterService;
import com.example.mhproject.service.MallService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    @Autowired private AdvertisementRepository adRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private MallService mallService;
    @Autowired private BloomFilterService bloomFilter;

    // 核心权限校验：必须是 ADMIN 角色才能操作
    private void checkAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) throw new RuntimeException("未登录！");
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("无权访问，您不是管理员！");
        }
    }

    // =================== 数据统计仪表盘 ===================
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard(HttpSession session) {
        checkAdmin(session);
        return mallService.getDashboardStats();
    }

    // =================== 广告管理 ===================
    @GetMapping("/ads/list")
    public List<Advertisement> getAllAds(HttpSession session) {
        checkAdmin(session);
        return adRepo.findAll();
    }

    @PostMapping("/ads/save")
    public Advertisement saveAd(@RequestBody Advertisement ad, HttpSession session) {
        checkAdmin(session);
        if (ad.getId() == null) ad.setCreateTime(LocalDateTime.now());
        ad.setUpdateTime(LocalDateTime.now());
        return adRepo.save(ad);
    }

    @PostMapping("/ads/delete")
    public String deleteAd(@RequestParam Long id, HttpSession session) {
        checkAdmin(session);
        adRepo.deleteById(id);
        return "删除成功";
    }

    @PostMapping("/ads/toggle")
    public String toggleAd(@RequestParam Long id, HttpSession session) {
        checkAdmin(session);
        Advertisement ad = adRepo.findById(id).orElseThrow(() -> new RuntimeException("广告不存在"));
        ad.setStatus(ad.getStatus() == 1 ? 0 : 1);
        ad.setUpdateTime(LocalDateTime.now());
        adRepo.save(ad);
        return "状态更新成功";
    }

    // =================== 商品管理 ===================
    // 获取所有商品
    @GetMapping("/products/list")
    public List<Product> getAllProducts(HttpSession session) {
        checkAdmin(session);
        return productRepo.findAll();
    }

    // 更新商品（含图片）/ 新增商品
    @PostMapping("/products/save")
    public Product saveProduct(@RequestBody Product product, HttpSession session) {
        checkAdmin(session);
        // 新增商品时默认上架 + 加入布隆过滤器
        if (product.getId() == null) {
            product.setStatus(1);
            product.setCreateTime(java.time.LocalDateTime.now());
        }
        Product saved = productRepo.save(product);
        bloomFilter.add(saved.getId());
        return saved;
    }

    // 删除商品
    @PostMapping("/products/delete")
    public String deleteProduct(@RequestParam Long id, HttpSession session) {
        checkAdmin(session);
        productRepo.deleteById(id);
        return "删除成功";
    }

    // 上下架切换：status=1 上架，status=0 下架
    @PostMapping("/products/toggleStatus")
    public String toggleProductStatus(@RequestParam Long id, HttpSession session) {
        checkAdmin(session);
        Product p = productRepo.findById(id).orElseThrow(() -> new RuntimeException("商品不存在"));
        p.setStatus(p.getStatus() == 1 ? 0 : 1);
        productRepo.save(p);
        return p.getStatus() == 1 ? "已上架" : "已下架";
    }

    // =================== 订单管理（发货） ===================
    // 全部订单
    @GetMapping("/orders/list")
    public List<Order> getAllOrders(HttpSession session) {
        checkAdmin(session);
        return mallService.getAllOrders();
    }

    // 发货（已付款 -> 已发货）
    @PostMapping("/orders/ship")
    public String shipOrder(@RequestParam Long orderId, HttpSession session) {
        checkAdmin(session);
        mallService.shipOrder(orderId);
        return "发货成功";
    }

    // =================== 退款审核 ===================
    // 全部退款申请
    @GetMapping("/refunds/list")
    public List<RefundRequest> getAllRefunds(HttpSession session) {
        checkAdmin(session);
        return mallService.getAllRefunds();
    }

    // 同意退款
    @PostMapping("/refunds/approve")
    public String approveRefund(@RequestParam Long refundId, HttpSession session) {
        checkAdmin(session);
        mallService.approveRefund(refundId);
        return "已同意退款";
    }

    // 驳回退款
    @PostMapping("/refunds/reject")
    public String rejectRefund(@RequestParam Long refundId, HttpSession session) {
        checkAdmin(session);
        mallService.rejectRefund(refundId);
        return "已驳回退款";
    }
}
