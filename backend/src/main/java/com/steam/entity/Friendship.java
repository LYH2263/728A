package com.steam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Friendship {
    private Long id;
    private Long userId;
    private Long friendId;
    private String status;
    private Long actionUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User friendUser;
}
