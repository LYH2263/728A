package com.steam.entity;

import com.steam.dto.UnlockedAchievementVO;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long userCouponId;
    private Long recipientId;
    private String giftMessage;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal couponDiscount;
    private BigDecimal payAmount;
    private String status;
    private LocalDateTime payTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<OrderItem> orderItems;
    private UserCoupon userCoupon;
    private User recipient;
    private List<UnlockedAchievementVO> unlockedAchievements;
}
