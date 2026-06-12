package com.steam.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundRequest {
    private Long id;
    private String refundNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long gameId;
    private String gameTitle;
    private String gameCover;
    private Long orderItemId;
    private BigDecimal orderItemPrice;
    private String reason;
    private String status;
    private Long reviewUserId;
    private String reviewRemark;
    private LocalDateTime reviewedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User user;
    private User reviewUser;
    private Order order;
}
