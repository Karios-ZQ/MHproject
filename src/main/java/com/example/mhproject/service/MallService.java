package com.example.mhproject.service;

import com.example.mhproject.entity.*;
import com.example.mhproject.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MallService {

    @Autowired private CartItemRepository cartRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private OrderRepository orderRepo;
    @Autowired private OrderItemRepository orderItemRepo;
    @Autowired private RefundRequestRepository refundRepo;
    @Autowired private AdvertisementRepository adRepo;
    @Autowired private UserAddressRepository addressRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private RedisStockService stockService;
    @Autowired private BloomFilterService bloomFilter;

    // ================= 0. 商品浏览 / 搜索 / 详情 =================

    // 商品列表：支持分类、关键词、排序
    public List<Product> getProducts(String category, String keyword, String sort) {
        List<Product> list;
        boolean hasCat = category != null && !category.trim().isEmpty();
        boolean hasKw = keyword != null && !keyword.trim().isEmpty();

        if (hasCat && hasKw) {
            list = productRepo.search(category.trim(), keyword.trim());
        } else if (hasCat) {
            list = productRepo.findByCategory(category.trim());
        } else if (hasKw) {
            list = productRepo.findByNameContaining(keyword.trim());
        } else {
            list = productRepo.findAll();
        }

        // 统一排序：price_asc / price_desc / newest / 默认
        Comparator<Product> cmp = Comparator.comparing(Product::getId, Comparator.reverseOrder());
        if (sort != null) {
            switch (sort) {
                case "price_asc": cmp = Comparator.comparing(Product::getPrice); break;
                case "price_desc": cmp = Comparator.comparing(Product::getPrice, Comparator.reverseOrder()); break;
                case "newest": cmp = Comparator.comparing(Product::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())); break;
                default: cmp = Comparator.comparing(Product::getId, Comparator.reverseOrder());
            }
        }
        // 只显示上架商品（status=1），下架商品对用户不可见
        return list.stream()
                .filter(p -> p.getStatus() == null || p.getStatus() == 1)
                .sorted(cmp).collect(Collectors.toList());
    }

    public Product getProductDetail(Long productId) {
        // 布隆过滤器拦截：不存在的商品直接拒绝，不查数据库（防缓存穿透）
        if (!bloomFilter.mightExist(productId)) {
            throw new RuntimeException("商品不存在");
        }
        return productRepo.findById(productId).orElseThrow(() -> new RuntimeException("商品不存在"));
    }

    public List<String> getCategories() {
        return productRepo.findAll().stream()
                .map(Product::getCategory)
                .filter(c -> c != null && !c.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    // ================= 1. 购物车模块 =================

    @Transactional
    public void addToCart(Long userId, Long productId, Integer quantity) {
        Product product = productRepo.findById(productId).orElseThrow(() -> new RuntimeException("商品不存在"));
        List<CartItem> existing = cartRepo.findByUserId(userId);
        for (CartItem item : existing) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + quantity);
                item.setUpdateTime(LocalDateTime.now());
                cartRepo.save(item);
                return;
            }
        }

        CartItem newItem = new CartItem();
        newItem.setUserId(userId);
        newItem.setProductId(productId);
        newItem.setMerchantId(product.getMerchantId());
        newItem.setQuantity(quantity);
        newItem.setChecked(true);
        newItem.setCreateTime(LocalDateTime.now());
        newItem.setUpdateTime(LocalDateTime.now());
        cartRepo.save(newItem);
    }

    public List<CartItem> getCart(Long userId) {
        List<CartItem> cartItems = cartRepo.findByUserId(userId);
        for (CartItem item : cartItems) {
            Product p = productRepo.findById(item.getProductId()).orElse(null);
            if (p != null) {
                item.setProductName(p.getName());
                item.setProductPrice(p.getPrice());
                item.setTotalPrice(p.getPrice().multiply(new BigDecimal(item.getQuantity())));
                item.setProductImage(p.getImageUrl() != null && !p.getImageUrl().isEmpty()
                        ? p.getImageUrl() : "https://via.placeholder.com/80");
            }
        }
        return cartItems;
    }

    @Transactional
    public void updateCartChecked(Long cartId, Boolean checked) {
        CartItem item = cartRepo.findById(cartId).orElseThrow(() -> new RuntimeException("购物车项不存在"));
        item.setChecked(checked);
        item.setUpdateTime(LocalDateTime.now());
        cartRepo.save(item);
    }

    @Transactional
    public void updateCartQuantity(Long cartId, Integer quantity) {
        if (quantity <= 0) throw new RuntimeException("数量不能小于1");
        CartItem item = cartRepo.findById(cartId).orElseThrow(() -> new RuntimeException("购物车项不存在"));
        item.setQuantity(quantity);
        item.setUpdateTime(LocalDateTime.now());
        cartRepo.save(item);
    }

    @Transactional
    public void deleteCartItem(Long cartId) {
        cartRepo.deleteById(cartId);
    }

    // ================= 2. 订单结算模块 =================

    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, Long addressId) {
        UserAddress address = addressRepo.findById(addressId).orElseThrow(() -> new RuntimeException("收货地址不存在"));
        if (!address.getUserId().equals(userId)) throw new RuntimeException("非法操作");

        List<CartItem> checkedItems = cartRepo.findByUserIdAndCheckedTrue(userId);
        if (checkedItems.isEmpty()) throw new RuntimeException("请至少勾选一件商品进行结算");

        // ============ Redis 预扣库存（原子，杜绝高并发超卖）============
        // 记录已经预扣成功的 (商品ID -> 数量)，任一步失败时整体回滚
        Map<Long, Integer> preDeducted = new HashMap<>();
        try {
            for (CartItem cart : checkedItems) {
                if (!stockService.preDeduct(cart.getProductId(), cart.getQuantity())) {
                    throw new RuntimeException("库存不足: 商品#" + cart.getProductId());
                }
                preDeducted.put(cart.getProductId(), cart.getQuantity());
            }
        } catch (Exception e) {
            // 预扣失败 / 库存不足：把已经预扣的库存全部补回
            preDeducted.forEach(stockService::compensate);
            throw e;
        }

        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());

        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setOrderStatus(0); // 待付款

        BigDecimal total = BigDecimal.ZERO;
        Long merchantId = null;
        List<OrderItem> orderItems = new ArrayList<>();

        try {
            for (CartItem cart : checkedItems) {
                Product p = productRepo.findById(cart.getProductId()).orElseThrow(() -> new RuntimeException("商品不存在"));
                if (p.getStock() < cart.getQuantity()) {
                    throw new RuntimeException("库存不足: " + p.getName());
                }
                p.setStock(p.getStock() - cart.getQuantity());
                productRepo.save(p);

                OrderItem oi = new OrderItem();
                oi.setProductId(p.getId());
                oi.setProductName(p.getName());
                oi.setPrice(p.getPrice());
                oi.setQuantity(cart.getQuantity());
                oi.setTotalPrice(p.getPrice().multiply(new BigDecimal(cart.getQuantity())));
                oi.setCreateTime(LocalDateTime.now());
                orderItems.add(oi);

                total = total.add(oi.getTotalPrice());
                merchantId = p.getMerchantId();
            }

            order.setMerchantId(merchantId);
            order.setTotalAmount(total);
            order.setPayAmount(total);

            Order savedOrder = orderRepo.save(order);

            for (OrderItem oi : orderItems) {
                oi.setOrderId(savedOrder.getId());
            }
            orderItemRepo.saveAll(orderItems);
            savedOrder.setItems(orderItems);

            cartRepo.deleteAll(checkedItems);
            return savedOrder;
        } catch (Exception e) {
            // 数据库扣减/落库失败：事务回滚的同时，把预扣的 Redis 库存补回，保持两边一致
            preDeducted.forEach(stockService::compensate);
            throw e;
        }
    }

    @Transactional
    public void payOrder(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));
        if (order.getOrderStatus() != 0) throw new RuntimeException("订单状态错误，无法支付");
        order.setOrderStatus(1);
        order.setUpdateTime(LocalDateTime.now());
        orderRepo.save(order);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));
        if (order.getOrderStatus() != 0) throw new RuntimeException("只能取消未付款订单");

        order.setOrderStatus(4);
        order.setUpdateTime(LocalDateTime.now());
        orderRepo.save(order);

        // 回滚库存（按 orderId 精确查询明细）：数据库与 Redis 同步归还
        List<OrderItem> orderItems = orderItemRepo.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            Product p = productRepo.findById(item.getProductId()).orElse(null);
            if (p != null) {
                p.setStock(p.getStock() + item.getQuantity());
                productRepo.save(p);
            }
            stockService.restore(item.getProductId(), item.getQuantity());
        }
    }

    // ================= 2.1 发货 / 收货（新） =================

    @Transactional
    public void shipOrder(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));
        if (order.getOrderStatus() != 1) throw new RuntimeException("只有已付款的订单才能发货");
        order.setOrderStatus(2); // 已发货
        order.setUpdateTime(LocalDateTime.now());
        orderRepo.save(order);
    }

    @Transactional
    public void confirmReceipt(Long userId, Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!order.getUserId().equals(userId)) throw new RuntimeException("非法操作");
        if (order.getOrderStatus() != 2) throw new RuntimeException("只有已发货的订单才能确认收货");
        order.setOrderStatus(3); // 已完成
        order.setUpdateTime(LocalDateTime.now());
        orderRepo.save(order);
    }

    public List<Order> getUserOrders(Long userId) {
        List<Order> orders = orderRepo.findByUserIdOrderByCreateTimeDesc(userId);
        for (Order order : orders) {
            order.setItems(orderItemRepo.findByOrderId(order.getId()));
        }
        return orders;
    }

    // 管理员查看全部订单
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepo.findAllByOrderByCreateTimeDesc();
        for (Order order : orders) {
            order.setItems(orderItemRepo.findByOrderId(order.getId()));
        }
        return orders;
    }

    // ================= 3. 售后 / 退款模块 =================

    @Transactional
    public void applyRefund(Long orderId, String reason) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));
        if (order.getOrderStatus() != 1 && order.getOrderStatus() != 2) {
            throw new RuntimeException("只有已付款或已发货的订单才能申请退款");
        }

        RefundRequest req = new RefundRequest();
        req.setOrderId(orderId);
        req.setUserId(order.getUserId());
        req.setMerchantId(order.getMerchantId());
        req.setRefundAmount(order.getPayAmount());
        req.setRefundReason(reason);
        req.setRefundStatus(0);
        req.setCreateTime(LocalDateTime.now());
        req.setUpdateTime(LocalDateTime.now());
        refundRepo.save(req);

        order.setOrderStatus(5);
        order.setUpdateTime(LocalDateTime.now());
        orderRepo.save(order);
    }

    // 管理员：全部退款申请
    public List<RefundRequest> getAllRefunds() {
        return refundRepo.findAllByOrderByCreateTimeDesc();
    }

    // 管理员：同意退款 -> 订单已退款(6)
    @Transactional
    public void approveRefund(Long refundId) {
        RefundRequest req = refundRepo.findById(refundId).orElseThrow(() -> new RuntimeException("退款申请不存在"));
        if (req.getRefundStatus() != 0) throw new RuntimeException("该申请已处理");
        req.setRefundStatus(1); // 已通过
        req.setUpdateTime(LocalDateTime.now());
        refundRepo.save(req);

        Order order = orderRepo.findById(req.getOrderId()).orElse(null);
        if (order != null) {
            order.setOrderStatus(6); // 已退款
            order.setUpdateTime(LocalDateTime.now());
            orderRepo.save(order);
        }
    }

    // 管理员：驳回退款 -> 订单回到已付款/已发货
    @Transactional
    public void rejectRefund(Long refundId) {
        RefundRequest req = refundRepo.findById(refundId).orElseThrow(() -> new RuntimeException("退款申请不存在"));
        if (req.getRefundStatus() != 0) throw new RuntimeException("该申请已处理");
        req.setRefundStatus(2); // 已驳回
        req.setUpdateTime(LocalDateTime.now());
        refundRepo.save(req);

        Order order = orderRepo.findById(req.getOrderId()).orElse(null);
        if (order != null) {
            // 恢复为已发货（若原本是已发货）否则已付款
            order.setOrderStatus(1);
            order.setUpdateTime(LocalDateTime.now());
            orderRepo.save(order);
        }
    }

    // ================= 4. 广告模块 =================

    public List<Advertisement> getActiveAds() {
        return adRepo.findByStatusOrderBySortOrderDesc(1);
    }

    // ================= 5. 后台统计 =================

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<Order> allOrders = orderRepo.findAll();
        List<Product> allProducts = productRepo.findAll();
        List<User> allUsers = userRepo.findAll();

        // 已完成交易的订单（已付款+已发货+已完成+退款中+已退款）
        List<Order> paidOrders = allOrders.stream()
                .filter(o -> o.getOrderStatus() >= 1 && o.getOrderStatus() <= 3).toList();
        BigDecimal totalSales = paidOrders.stream()
                .map(Order::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("orderCount", allOrders.size());
        stats.put("pendingShipCount", allOrders.stream().filter(o -> o.getOrderStatus() == 1).count());
        stats.put("productCount", allProducts.size());
        stats.put("userCount", allUsers.size());
        stats.put("totalSales", totalSales);
        stats.put("refundPendingCount", refundRepo.findAll().stream().filter(r -> r.getRefundStatus() == 0).count());

        // 库存预警（<=5 视为告急）
        List<Product> lowStock = allProducts.stream()
                .filter(p -> p.getStock() != null && p.getStock() <= 5)
                .sorted(Comparator.comparing(Product::getStock))
                .collect(Collectors.toList());
        stats.put("lowStock", lowStock);

        // 热销榜：按订单明细累计销量
        Map<Long, Long> salesMap = new HashMap<>();
        Map<Long, BigDecimal> revenueMap = new HashMap<>();
        Map<Long, String> nameMap = new HashMap<>();
        for (OrderItem oi : orderItemRepo.findAll()) {
            salesMap.merge(oi.getProductId(), oi.getQuantity().longValue(), Long::sum);
            revenueMap.merge(oi.getProductId(), oi.getTotalPrice(), BigDecimal::add);
            nameMap.putIfAbsent(oi.getProductId(), oi.getProductName());
        }
        List<Map<String, Object>> hotProducts = salesMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId", e.getKey());
                    m.put("name", nameMap.get(e.getKey()));
                    m.put("sold", e.getValue());
                    m.put("revenue", revenueMap.get(e.getKey()));
                    return m;
                }).collect(Collectors.toList());
        stats.put("hotProducts", hotProducts);

        // 订单状态分布
        Map<Integer, Long> statusDist = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getOrderStatus, Collectors.counting()));
        stats.put("statusDist", statusDist);

        return stats;
    }
}
