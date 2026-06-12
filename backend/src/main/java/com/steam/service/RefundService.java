package com.steam.service;

import com.steam.entity.*;
import com.steam.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final int MAX_PLAYTIME_MINUTES = 120;
    private static final int MAX_DAYS_SINCE_PURCHASE = 14;

    private final RefundRequestMapper refundRequestMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final GameMapper gameMapper;
    private final UserLibraryMapper userLibraryMapper;

    @Transactional
    public RefundRequest applyRefund(Long userId, Long orderItemId, String reason) {
        OrderItem orderItem = orderMapper.findOrderItemById(orderItemId);
        if (orderItem == null) {
            throw new RuntimeException("订单项不存在");
        }

        Order order = orderMapper.findById(orderItem.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        checkRefundEligibility(order, orderItem, userId);

        int activeCount = refundRequestMapper.countActiveByOrderItemId(orderItemId);
        if (activeCount > 0) {
            throw new RuntimeException("该游戏已存在退款申请，请等待审核");
        }

        Game game = gameMapper.findByIdIgnoreStatus(orderItem.getGameId());

        RefundRequest request = new RefundRequest();
        request.setRefundNo(generateRefundNo());
        request.setOrderId(order.getId());
        request.setOrderNo(order.getOrderNo());
        request.setUserId(userId);
        request.setGameId(orderItem.getGameId());
        request.setGameTitle(game != null ? game.getTitle() : orderItem.getGameTitle());
        request.setGameCover(game != null ? game.getCoverImage() : orderItem.getGameCover());
        request.setOrderItemId(orderItemId);
        request.setOrderItemPrice(orderItem.getPrice());
        request.setReason(reason);
        request.setStatus("PENDING");

        refundRequestMapper.insert(request);

        log.info("退款申请创建成功: refundNo={}, orderNo={}, userId={}, gameId={}",
                request.getRefundNo(), order.getOrderNo(), userId, orderItem.getGameId());
        return request;
    }

    public void checkRefundEligibility(Order order, OrderItem orderItem, Long userId) {
        String orderStatus = order.getStatus();
        if (!"PAID".equals(orderStatus) && !"COMPLETED".equals(orderStatus)) {
            throw new RuntimeException("仅已支付或已完成的订单可申请退款");
        }

        if (orderItem.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("免费游戏不支持退款");
        }

        LocalDateTime payTime = order.getPayTime();
        if (payTime == null) {
            throw new RuntimeException("订单支付时间异常");
        }

        LocalDateTime now = LocalDateTime.now();
        long daysSincePurchase = java.time.Duration.between(payTime, now).toDays();
        if (daysSincePurchase > MAX_DAYS_SINCE_PURCHASE) {
            throw new RuntimeException("购买已超过" + MAX_DAYS_SINCE_PURCHASE + "天，无法申请退款");
        }

        UserLibrary library = userLibraryMapper.findByUserIdAndGameId(userId, orderItem.getGameId());
        if (library != null && library.getPlayTime() != null && library.getPlayTime() >= MAX_PLAYTIME_MINUTES) {
            throw new RuntimeException("游玩时长已超过" + MAX_PLAYTIME_MINUTES + "分钟，无法申请退款");
        }
    }

    public boolean isRefundEligible(Long userId, Long orderItemId) {
        try {
            OrderItem orderItem = orderMapper.findOrderItemById(orderItemId);
            if (orderItem == null) return false;
            Order order = orderMapper.findById(orderItem.getOrderId());
            if (order == null) return false;
            if (!order.getUserId().equals(userId)) return false;
            int activeCount = refundRequestMapper.countActiveByOrderItemId(orderItemId);
            if (activeCount > 0) return false;
            checkRefundEligibility(order, orderItem, userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public RefundRequest reviewRefund(Long adminUserId, Long refundId, String action, String remark) {
        RefundRequest request = refundRequestMapper.findById(refundId);
        if (request == null) {
            throw new RuntimeException("退款申请不存在");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("该退款申请已处理");
        }

        if ("APPROVE".equalsIgnoreCase(action)) {
            return approveRefund(request, adminUserId, remark);
        } else if ("REJECT".equalsIgnoreCase(action)) {
            LocalDateTime now = LocalDateTime.now();
            refundRequestMapper.updateReview(refundId, "REJECTED", adminUserId, remark, now);
            request.setStatus("REJECTED");
            request.setReviewUserId(adminUserId);
            request.setReviewRemark(remark);
            request.setReviewedAt(now);
            log.info("退款申请已拒绝: refundNo={}, adminUserId={}, remark={}", request.getRefundNo(), adminUserId, remark);
            return request;
        } else {
            throw new RuntimeException("无效的审核操作");
        }
    }

    @Transactional
    protected RefundRequest approveRefund(RefundRequest request, Long adminUserId, String remark) {
        LocalDateTime now = LocalDateTime.now();
        refundRequestMapper.updateReview(request.getId(), "APPROVED", adminUserId, remark, now);

        Order order = orderMapper.findById(request.getOrderId());
        if (order == null) {
            throw new RuntimeException("关联订单不存在");
        }

        OrderItem orderItem = orderMapper.findOrderItemById(request.getOrderItemId());
        if (orderItem == null) {
            throw new RuntimeException("订单项不存在");
        }

        BigDecimal refundAmount = calculateRefundAmount(order, orderItem);

        User user = userMapper.findById(request.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        BigDecimal newBalance = user.getBalance() != null ? user.getBalance().add(refundAmount) : refundAmount;
        userMapper.updateBalance(request.getUserId(), newBalance);

        userLibraryMapper.deleteByUserIdAndGameId(request.getUserId(), request.getGameId());

        gameMapper.increaseStock(request.getGameId());

        refundRequestMapper.markRefunded(request.getId(), now);

        updateOrderRefundStatus(order.getId());

        request.setStatus("REFUNDED");
        request.setReviewUserId(adminUserId);
        request.setReviewRemark(remark);
        request.setReviewedAt(now);
        request.setRefundedAt(now);

        log.info("退款处理完成: refundNo={}, 退款金额={}, orderNo={}, userId={}, gameId={}",
                request.getRefundNo(), refundAmount, order.getOrderNo(), request.getUserId(), request.getGameId());
        return request;
    }

    protected BigDecimal calculateRefundAmount(Order order, OrderItem orderItem) {
        List<OrderItem> allItems = orderMapper.findOrderItemsByOrderId(order.getId());
        if (allItems == null || allItems.isEmpty()) {
            return orderItem.getPrice();
        }

        BigDecimal totalItemPrice = BigDecimal.ZERO;
        for (OrderItem item : allItems) {
            totalItemPrice = totalItemPrice.add(item.getPrice());
        }

        if (totalItemPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal payAmount = order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO;
        BigDecimal ratio = payAmount.divide(totalItemPrice, 4, RoundingMode.HALF_UP);
        return orderItem.getPrice().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    protected void updateOrderRefundStatus(Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) return;

        List<OrderItem> orderItems = orderMapper.findOrderItemsByOrderId(orderId);
        if (orderItems == null || orderItems.isEmpty()) return;

        int refundedCount = 0;
        for (OrderItem item : orderItems) {
            int active = refundRequestMapper.countActiveByOrderItemId(item.getId());
            if (active > 0) {
                refundedCount++;
            }
        }

        if (refundedCount == orderItems.size()) {
            orderMapper.updateStatusOnly(orderId, "FULL_REFUND");
        } else if (refundedCount > 0) {
            orderMapper.updateStatusOnly(orderId, "PARTIAL_REFUND");
        }
    }

    public List<RefundRequest> getUserRefunds(Long userId) {
        List<RefundRequest> refunds = refundRequestMapper.findByUserId(userId);
        for (RefundRequest r : refunds) {
            enrichRefund(r);
        }
        return refunds;
    }

    public List<RefundRequest> getAllRefunds(String status) {
        List<RefundRequest> refunds;
        if (status != null && !status.isEmpty()) {
            refunds = refundRequestMapper.findByStatus(status);
        } else {
            refunds = refundRequestMapper.findAll();
        }
        for (RefundRequest r : refunds) {
            enrichRefund(r);
        }
        return refunds;
    }

    public RefundRequest getRefundDetail(Long userId, Long refundId) {
        RefundRequest request = refundRequestMapper.findById(refundId);
        if (request == null) {
            throw new RuntimeException("退款申请不存在");
        }
        if (!request.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看此退款申请");
        }
        enrichRefund(request);
        return request;
    }

    public RefundRequest getRefundDetailAdmin(Long refundId) {
        RefundRequest request = refundRequestMapper.findById(refundId);
        if (request == null) {
            throw new RuntimeException("退款申请不存在");
        }
        enrichRefund(request);
        return request;
    }

    private void enrichRefund(RefundRequest r) {
        if (r.getUserId() != null) {
            User u = userMapper.findById(r.getUserId());
            if (u != null) {
                u.setPassword(null);
                r.setUser(u);
            }
        }
        if (r.getReviewUserId() != null) {
            User ru = userMapper.findById(r.getReviewUserId());
            if (ru != null) {
                ru.setPassword(null);
                r.setReviewUser(ru);
            }
        }
        if (r.getOrderId() != null) {
            Order o = orderMapper.findById(r.getOrderId());
            if (o != null) {
                o.setOrderItems(orderMapper.findOrderItemsByOrderId(o.getId()));
                r.setOrder(o);
            }
        }
    }

    private String generateRefundNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "REF" + timestamp + uuid;
    }
}
