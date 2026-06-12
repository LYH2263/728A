package com.steam.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Coupon {
    private Long id;
    private String name;
    private String code;
    private String type;
    private BigDecimal value;
    private BigDecimal minAmount;
    private Long categoryId;
    private Integer totalCount;
    private Integer claimedCount;
    private Integer perUserLimit;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
