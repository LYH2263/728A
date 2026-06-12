package com.steam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Activity {
    private Long id;
    private Long userId;
    private String type;
    private Long gameId;
    private String gameTitle;
    private String gameCover;
    private Long achievementId;
    private String achievementName;
    private Long reviewId;
    private Integer reviewRating;
    private String reviewContent;
    private String metadata;
    private LocalDateTime createdAt;

    private User user;
}
