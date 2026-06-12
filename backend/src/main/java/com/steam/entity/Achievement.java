package com.steam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Achievement {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String category;
    private Integer targetValue;
    private Integer isProgress;
    private Integer rarity;
    private Integer points;
    private String eventType;
    private String ruleConfig;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
