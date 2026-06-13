package com.steam.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Gift {
    private Long id;
    private String giftNo;
    private Long senderId;
    private Long recipientId;
    private Long gameId;
    private String gameTitle;
    private String gameCover;
    private Long orderId;
    private Long orderItemId;
    private BigDecimal pricePaid;
    private String status;
    private String message;
    private LocalDateTime claimedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User sender;
    private User recipient;
    private Game game;
}
