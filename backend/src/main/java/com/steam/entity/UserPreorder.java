package com.steam.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserPreorder {
    private Long id;
    private Long userId;
    private Long gameId;
    private Long orderId;
    private Long orderItemId;
    private BigDecimal pricePaid;
    private String releaseStatus;  // PREORDER / CROWDFUNDING
    private String status;         // PENDING_RELEASE / RELEASED / CANCELLED
    private LocalDateTime convertedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Game game;
    private Order order;
    private User user;
}
