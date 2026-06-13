package com.steam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StockChangeLog {
    private Long id;
    private Long gameId;
    private Long adminId;
    private String adminUsername;
    private Integer stockBefore;
    private Integer stockAfter;
    private String changeType;
    private String remark;
    private LocalDateTime createdAt;
}
