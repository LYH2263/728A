package com.steam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserAchievement {
    private Long id;
    private Long userId;
    private Long achievementId;
    private Integer progress;
    private Integer targetValue;
    private Integer isUnlocked;
    private LocalDateTime unlockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Achievement achievement;
}
