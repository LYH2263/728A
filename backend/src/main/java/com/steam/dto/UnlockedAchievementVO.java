package com.steam.dto;

import lombok.Data;

@Data
public class UnlockedAchievementVO {
    private Long achievementId;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String category;
    private Integer rarity;
    private Integer points;
    private String unlockedAt;
}
