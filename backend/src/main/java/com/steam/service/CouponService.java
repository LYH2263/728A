package com.steam.service;

import com.steam.entity.*;
import com.steam.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final CategoryMapper categoryMapper;
    private final GameMapper gameMapper;
    private final UserMapper userMapper;

    @Transactional
    public UserCoupon claimCoupon(Long userId, String code) {
        Coupon coupon = couponMapper.findByCode(code);
        if (coupon == null) {
            throw new RuntimeException("优惠券不存在");
        }
        if (coupon.getStatus() != 1) {
            throw new RuntimeException("优惠券已失效");
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidStart())) {
            throw new RuntimeException("优惠券尚未开始");
        }
        if (now.isAfter(coupon.getValidEnd())) {
            throw new RuntimeException("优惠券已过期");
        }

        int userCount = userCouponMapper.countByUserIdAndCouponId(userId, coupon.getId());
        if (userCount >= coupon.getPerUserLimit()) {
            throw new RuntimeException("您已领取过该优惠券");
        }

        if (coupon.getTotalCount() != -1 && coupon.getClaimedCount() >= coupon.getTotalCount()) {
            throw new RuntimeException("优惠券已领完");
        }

        int rows = couponMapper.increaseClaimedCount(coupon.getId());
        if (rows == 0) {
            throw new RuntimeException("优惠券已领完");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setStatus("UNUSED");
        userCouponMapper.insert(userCoupon);

        log.info("用户领取优惠券成功: userId={}, couponId={}", userId, coupon.getId());
        return userCoupon;
    }

    public List<UserCoupon> getUserCoupons(Long userId, String status) {
        List<UserCoupon> userCoupons;
        if (status != null && !status.isEmpty()) {
            userCoupons = userCouponMapper.findByUserIdAndStatus(userId, status);
        } else {
            userCoupons = userCouponMapper.findByUserId(userId);
        }
        
        for (UserCoupon uc : userCoupons) {
            Coupon coupon = couponMapper.findById(uc.getCouponId());
            if (coupon != null && "UNUSED".equals(uc.getStatus())) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(coupon.getValidEnd())) {
                    uc.setStatus("EXPIRED");
                }
            }
            uc.setCoupon(coupon);
        }
        return userCoupons;
    }

    public List<UserCoupon> getAvailableCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponMapper.findAvailableByUserId(userId);
        for (UserCoupon uc : userCoupons) {
            uc.setCoupon(couponMapper.findById(uc.getCouponId()));
        }
        return userCoupons;
    }

    public List<UserCoupon> getApplicableCoupons(Long userId, List<Long> gameIds) {
        List<UserCoupon> availableCoupons = getAvailableCoupons(userId);
        List<UserCoupon> applicable = new ArrayList<>();
        List<Game> games = new ArrayList<>();
        
        for (Long gameId : gameIds) {
            Game game = gameMapper.findById(gameId);
            if (game != null) {
                games.add(game);
            }
        }
        
        if (games.isEmpty()) {
            return applicable;
        }

        BigDecimal totalDiscountPrice = calculateTotalDiscountPrice(games);

        for (UserCoupon uc : availableCoupons) {
            Coupon coupon = uc.getCoupon();
            if (coupon == null) continue;

            try {
                CouponCalcResult result = calculateCouponDiscount(coupon, games, totalDiscountPrice);
                if (result.isApplicable()) {
                    uc.setCoupon(coupon);
                    applicable.add(uc);
                }
            } catch (Exception e) {
                // skip invalid coupon
            }
        }

        applicable.sort((a, b) -> {
            BigDecimal discountA = calculateCouponDiscount(a.getCoupon(), games, totalDiscountPrice).getDiscountAmount();
            BigDecimal discountB = calculateCouponDiscount(b.getCoupon(), games, totalDiscountPrice).getDiscountAmount();
            return discountB.compareTo(discountA);
        });

        return applicable;
    }

    public CouponCalcResult calculateCouponDiscount(Coupon coupon, List<Game> games, BigDecimal totalDiscountPrice) {
        CouponCalcResult result = new CouponCalcResult();
        result.setApplicable(false);
        result.setDiscountAmount(BigDecimal.ZERO);

        if (coupon == null || games == null || games.isEmpty()) {
            return result;
        }

        BigDecimal minAmount = coupon.getMinAmount() != null ? coupon.getMinAmount() : BigDecimal.ZERO;

        if ("CATEGORY".equals(coupon.getType()) && coupon.getCategoryId() != null) {
            BigDecimal categoryTotal = BigDecimal.ZERO;
            for (Game game : games) {
                List<Category> categories = categoryMapper.findByGameId(game.getId());
                boolean match = categories.stream().anyMatch(c -> c.getId().equals(coupon.getCategoryId()));
                if (match) {
                    BigDecimal price = game.getDiscountPrice() != null ? game.getDiscountPrice() : game.getOriginalPrice();
                    categoryTotal = categoryTotal.add(price);
                }
            }

            if (categoryTotal.compareTo(minAmount) < 0) {
                return result;
            }

            result.setApplicable(true);
            result.setCategorySubtotal(categoryTotal);
            result.setDiscountAmount(calcDiscountByType(coupon, categoryTotal));
        } else {
            if (totalDiscountPrice.compareTo(minAmount) < 0) {
                return result;
            }

            result.setApplicable(true);
            result.setDiscountAmount(calcDiscountByType(coupon, totalDiscountPrice));
        }

        return result;
    }

    private BigDecimal calcDiscountByType(Coupon coupon, BigDecimal baseAmount) {
        if ("FULL_REDUCTION".equals(coupon.getType())) {
            return coupon.getValue();
        } else if ("DISCOUNT".equals(coupon.getType()) || "CATEGORY".equals(coupon.getType())) {
            BigDecimal value = coupon.getValue();
            if (value.compareTo(BigDecimal.valueOf(100)) >= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal discountPercent = value.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
            BigDecimal discount = baseAmount.multiply(BigDecimal.ONE.subtract(discountPercent));
            return discount.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTotalDiscountPrice(List<Game> games) {
        BigDecimal total = BigDecimal.ZERO;
        for (Game game : games) {
            BigDecimal price = game.getDiscountPrice() != null ? game.getDiscountPrice() : game.getOriginalPrice();
            total = total.add(price);
        }
        return total;
    }

    @Transactional
    public void useCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = userCouponMapper.findById(userCouponId);
        if (userCoupon == null) {
            throw new RuntimeException("优惠券不存在");
        }
        if (!"UNUSED".equals(userCoupon.getStatus())) {
            throw new RuntimeException("优惠券已被使用或已过期");
        }

        int rows = userCouponMapper.markAsUsed(userCouponId, orderId, LocalDateTime.now());
        if (rows == 0) {
            throw new RuntimeException("优惠券使用失败，请刷新后重试");
        }
        log.info("优惠券使用成功: userCouponId={}, orderId={}", userCouponId, orderId);
    }

    @Transactional
    public void returnCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponMapper.findById(userCouponId);
        if (userCoupon == null) {
            return;
        }
        if (!"USED".equals(userCoupon.getStatus())) {
            return;
        }

        Coupon coupon = couponMapper.findById(userCoupon.getCouponId());
        if (coupon != null && LocalDateTime.now().isAfter(coupon.getValidEnd())) {
            return;
        }

        userCouponMapper.resetStatus(userCouponId);
        log.info("优惠券退还成功: userCouponId={}", userCouponId);
    }

    public Coupon getCouponByCode(String code) {
        return couponMapper.findByCode(code);
    }

    public UserCoupon getUserCoupon(Long userCouponId) {
        UserCoupon uc = userCouponMapper.findById(userCouponId);
        if (uc != null) {
            uc.setCoupon(couponMapper.findById(uc.getCouponId()));
        }
        return uc;
    }

    public static class CouponCalcResult {
        private boolean applicable;
        private BigDecimal discountAmount;
        private BigDecimal categorySubtotal;

        public boolean isApplicable() { return applicable; }
        public void setApplicable(boolean applicable) { this.applicable = applicable; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
        public BigDecimal getCategorySubtotal() { return categorySubtotal; }
        public void setCategorySubtotal(BigDecimal categorySubtotal) { this.categorySubtotal = categorySubtotal; }
    }
}
