package com.steam.service;

import com.steam.entity.*;
import com.steam.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final GameMapper gameMapper;
    private final UserMapper userMapper;
    private final CartMapper cartMapper;
    private final UserLibraryMapper userLibraryMapper;
    private final WishlistMapper wishlistMapper;
    private final CouponService couponService;
    private final UserCouponMapper userCouponMapper;
    private final CouponMapper couponMapper;
    private final AchievementService achievementService;
    private final ActivityService activityService;
    private final UserPreorderMapper userPreorderMapper;
    private final WalletService walletService;
    private final GiftService giftService;

    @Transactional
    public Order createOrder(Long userId, List<Long> gameIds, Long userCouponId,
                             Long recipientId, String giftMessage) {
        if (gameIds == null || gameIds.isEmpty()) {
            throw new RuntimeException("请选择要购买的游戏");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        boolean isGift = recipientId != null;
        Long targetUserId = isGift ? recipientId : userId;

        if (isGift) {
            if (userId.equals(recipientId)) {
                throw new RuntimeException("不能赠送给自己");
            }
            User recipient = userMapper.findById(recipientId);
            if (recipient == null) {
                throw new RuntimeException("收礼用户不存在");
            }
            if (gameIds.size() > 1) {
                throw new RuntimeException("赠送礼物每次只能赠送一款游戏");
            }
        }

        List<Game> games = new ArrayList<>();
        for (Long gameId : gameIds) {
            Game game = gameMapper.findById(gameId);
            if (game == null) {
                throw new RuntimeException("游戏不存在: " + gameId);
            }
            if (userLibraryMapper.existsByUserIdAndGameId(targetUserId, gameId)) {
                if (isGift) {
                    throw new RuntimeException("收礼用户已拥有游戏: " + game.getTitle());
                } else {
                    throw new RuntimeException("您已拥有游戏: " + game.getTitle());
                }
            }
            if (userPreorderMapper.existsPendingByUserIdAndGameId(targetUserId, gameId)) {
                if (isGift) {
                    throw new RuntimeException("收礼用户已预购游戏: " + game.getTitle());
                } else {
                    throw new RuntimeException("您已预购游戏: " + game.getTitle());
                }
            }
            if (game.getStock() <= 0) {
                throw new RuntimeException("游戏库存不足: " + game.getTitle());
            }
            games.add(game);
        }

        BigDecimal originalTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        for (Game game : games) {
            originalTotal = originalTotal.add(game.getOriginalPrice());
            BigDecimal price = game.getDiscountPrice() != null ? game.getDiscountPrice() : game.getOriginalPrice();
            discountTotal = discountTotal.add(price);
        }

        BigDecimal gameDiscount = originalTotal.subtract(discountTotal);
        BigDecimal couponDiscount = BigDecimal.ZERO;
        UserCoupon useCoupon = null;

        if (userCouponId != null && userCouponId > 0) {
            UserCoupon userCoupon = userCouponMapper.findById(userCouponId);
            if (userCoupon == null) {
                throw new RuntimeException("优惠券不存在");
            }
            if (!userCoupon.getUserId().equals(userId)) {
                throw new RuntimeException("无权使用此优惠券");
            }
            if (!"UNUSED".equals(userCoupon.getStatus())) {
                throw new RuntimeException("优惠券已使用或已过期");
            }

            Coupon coupon = couponMapper.findById(userCoupon.getCouponId());
            if (coupon == null) {
                throw new RuntimeException("优惠券信息异常");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(coupon.getValidStart()) || now.isAfter(coupon.getValidEnd())) {
                throw new RuntimeException("优惠券已过期");
            }

            CouponService.CouponCalcResult calcResult =
                    couponService.calculateCouponDiscount(coupon, games, discountTotal);

            if (!calcResult.isApplicable()) {
                throw new RuntimeException("不满足优惠券使用条件");
            }

            couponDiscount = calcResult.getDiscountAmount();
            useCoupon = userCoupon;
        }

        BigDecimal totalDiscount = gameDiscount.add(couponDiscount);
        BigDecimal payAmount = discountTotal.subtract(couponDiscount);
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
            couponDiscount = discountTotal;
            totalDiscount = originalTotal;
        }

        String orderNo = generateOrderNo();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setRecipientId(recipientId);
        order.setGiftMessage(giftMessage);
        order.setTotalAmount(originalTotal);
        order.setDiscountAmount(totalDiscount);
        order.setCouponDiscount(couponDiscount);
        order.setPayAmount(payAmount);
        order.setStatus("PENDING");
        if (useCoupon != null) {
            order.setUserCouponId(useCoupon.getId());
        }
        orderMapper.insert(order);

        for (Game game : games) {
            BigDecimal price = game.getDiscountPrice() != null ? game.getDiscountPrice() : game.getOriginalPrice();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setGameId(game.getId());
            orderItem.setGameTitle(game.getTitle());
            orderItem.setGameCover(game.getCoverImage());
            orderItem.setPrice(price);
            orderItem.setQuantity(1);
            orderMapper.insertOrderItem(orderItem);
        }

        order.setOrderItems(orderMapper.findOrderItemsByOrderId(order.getId()));
        if (useCoupon != null) {
            useCoupon.setCoupon(couponMapper.findById(useCoupon.getCouponId()));
            order.setUserCoupon(useCoupon);
        }

        log.info("订单创建成功: {}, 用户: {}, 金额: {}, 优惠券抵扣: {}", orderNo, userId, payAmount, couponDiscount);
        return order;
    }

    @Transactional
    public Order payOrder(Long userId, String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        List<OrderItem> orderItems = orderMapper.findOrderItemsByOrderId(order.getId());

        BigDecimal recalculatedAmount = BigDecimal.ZERO;
        BigDecimal originalTotal = BigDecimal.ZERO;
        List<Game> games = new ArrayList<>();

        for (OrderItem item : orderItems) {
            Game game = gameMapper.findById(item.getGameId());
            if (game == null) {
                throw new RuntimeException("游戏已下架: " + item.getGameTitle());
            }
            BigDecimal currentPrice = game.getDiscountPrice() != null ? game.getDiscountPrice() : game.getOriginalPrice();
            if (currentPrice.compareTo(item.getPrice()) != 0) {
                throw new RuntimeException("游戏价格已变动，请重新下单");
            }
            recalculatedAmount = recalculatedAmount.add(currentPrice);
            originalTotal = originalTotal.add(game.getOriginalPrice());
            games.add(game);
        }

        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (order.getUserCouponId() != null) {
            UserCoupon userCoupon = userCouponMapper.findById(order.getUserCouponId());
            if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
                throw new RuntimeException("优惠券信息异常");
            }
            if (!"UNUSED".equals(userCoupon.getStatus())) {
                throw new RuntimeException("优惠券已被使用，请重新下单");
            }

            Coupon coupon = couponMapper.findById(userCoupon.getCouponId());
            if (coupon == null) {
                throw new RuntimeException("优惠券不存在");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(coupon.getValidStart()) || now.isAfter(coupon.getValidEnd())) {
                throw new RuntimeException("优惠券已过期");
            }

            CouponService.CouponCalcResult calcResult =
                    couponService.calculateCouponDiscount(coupon, games, recalculatedAmount);

            if (!calcResult.isApplicable()) {
                throw new RuntimeException("不满足优惠券使用条件");
            }

            couponDiscount = calcResult.getDiscountAmount();

            couponService.useCoupon(userCoupon.getId(), order.getId());
        }

        BigDecimal finalPayAmount = recalculatedAmount.subtract(couponDiscount);
        if (finalPayAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalPayAmount = BigDecimal.ZERO;
        }

        User user = userMapper.findById(userId);
        if (user.getBalance().compareTo(finalPayAmount) < 0) {
            throw new RuntimeException("余额不足，请先充值");
        }

        boolean isGift = order.getRecipientId() != null;
        Long recipientId = order.getRecipientId();
        String giftMessage = order.getGiftMessage();

        walletService.purchase(userId, finalPayAmount, orderNo,
                isGift ? "赠送游戏，订单号: " + orderNo : "购买游戏，订单号: " + orderNo);

        // 重查订单项以获得每个item的id
        List<OrderItem> finalOrderItems = orderMapper.findOrderItemsByOrderId(order.getId());

        for (OrderItem item : finalOrderItems) {
            int rows = gameMapper.decreaseStock(item.getGameId());
            if (rows == 0) {
                throw new RuntimeException("游戏库存不足: " + item.getGameTitle());
            }

            Game game = gameMapper.findRawById(item.getGameId());
            String releaseStatus = game.getReleaseStatus() == null ? "RELEASED" : game.getReleaseStatus();

            if (isGift) {
                if (userLibraryMapper.existsByUserIdAndGameId(recipientId, item.getGameId())) {
                    throw new RuntimeException("收礼用户已拥有游戏: " + item.getGameTitle());
                }
                if (userPreorderMapper.existsPendingByUserIdAndGameId(recipientId, item.getGameId())) {
                    throw new RuntimeException("收礼用户已预购游戏: " + item.getGameTitle());
                }
                giftService.createGift(userId, recipientId, item, order.getId(), giftMessage);

                if ("CROWDFUNDING".equals(releaseStatus)) {
                    gameMapper.increaseCrowdfunding(item.getGameId(), item.getPrice());
                    gameMapper.markCrowdfundingSuccessIfGoalReached(item.getGameId());
                }
            } else {
                if ("RELEASED".equals(releaseStatus)) {
                    UserLibrary library = new UserLibrary();
                    library.setUserId(userId);
                    library.setGameId(item.getGameId());
                    library.setOrderId(order.getId());
                    userLibraryMapper.insert(library);
                } else {
                    UserPreorder preorder = new UserPreorder();
                    preorder.setUserId(userId);
                    preorder.setGameId(item.getGameId());
                    preorder.setOrderId(order.getId());
                    preorder.setOrderItemId(item.getId());
                    preorder.setPricePaid(item.getPrice());
                    preorder.setReleaseStatus(releaseStatus);
                    userPreorderMapper.insert(preorder);

                    if ("CROWDFUNDING".equals(releaseStatus)) {
                        gameMapper.increaseCrowdfunding(item.getGameId(), item.getPrice());
                        gameMapper.markCrowdfundingSuccessIfGoalReached(item.getGameId());
                    }
                }
            }

            cartMapper.deleteByUserIdAndGameId(userId, item.getGameId());
            wishlistMapper.deleteByUserIdAndGameId(userId, item.getGameId());
        }

        LocalDateTime now = LocalDateTime.now();
        orderMapper.updateStatus(order.getId(), "PAID", now);
        order.setStatus("PAID");
        order.setPayTime(now);
        order.setOrderItems(orderItems);
        order.setPayAmount(finalPayAmount);
        order.setDiscountAmount(originalTotal.subtract(finalPayAmount));
        order.setCouponDiscount(couponDiscount);

        if (order.getUserCouponId() != null) {
            UserCoupon uc = userCouponMapper.findById(order.getUserCouponId());
            if (uc != null) {
                uc.setCoupon(couponMapper.findById(uc.getCouponId()));
                order.setUserCoupon(uc);
            }
        }

        log.info("订单支付成功: {}, 用户: {}, 实付: {}", orderNo, userId, finalPayAmount);

        try {
            achievementService.triggerOrderPaid(userId, order.getId());
        } catch (Exception e) {
            log.error("触发成就计算失败: orderId={}, userId={}", order.getId(), userId, e);
        }

        try {
            activityService.createPurchaseActivity(userId, order.getId());
        } catch (Exception e) {
            log.error("创建购买动态失败: orderId={}, userId={}", order.getId(), userId, e);
        }

        return order;
    }

    @Transactional
    public void cancelOrder(Long userId, String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("只能取消待支付的订单");
        }

        if (order.getUserCouponId() != null) {
            couponService.returnCoupon(order.getUserCouponId());
        }

        orderMapper.updateStatus(order.getId(), "CANCELLED", null);
        log.info("订单取消成功: {}, 用户: {}", orderNo, userId);
    }

    public List<Order> getUserOrders(Long userId) {
        List<Order> orders = orderMapper.findByUserId(userId);
        for (Order order : orders) {
            order.setOrderItems(orderMapper.findOrderItemsByOrderId(order.getId()));
            if (order.getUserCouponId() != null) {
                UserCoupon uc = userCouponMapper.findById(order.getUserCouponId());
                if (uc != null) {
                    uc.setCoupon(couponMapper.findById(uc.getCouponId()));
                    order.setUserCoupon(uc);
                }
            }
        }
        return orders;
    }

    public Order getOrderDetail(Long userId, String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看此订单");
        }
        order.setOrderItems(orderMapper.findOrderItemsByOrderId(order.getId()));
        if (order.getUserCouponId() != null) {
            UserCoupon uc = userCouponMapper.findById(order.getUserCouponId());
            if (uc != null) {
                uc.setCoupon(couponMapper.findById(uc.getCouponId()));
                order.setUserCoupon(uc);
            }
        }
        return order;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "ORD" + timestamp + uuid;
    }
}
