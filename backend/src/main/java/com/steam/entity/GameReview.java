package com.steam.entity;

import com.steam.dto.UnlockedAchievementVO;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GameReview {
    private Long id;
    private Long userId;
    private Long gameId;
    private Integer rating;
    private String content;
    private Integer isRecommend;
    private Integer helpfulCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User user;
    private Game game;
    private List<UnlockedAchievementVO> unlockedAchievements;
}
