package com.steam.dto;

import lombok.Data;
import java.util.List;

@Data
public class StockAdjustDTO {
    private List<Long> gameIds;
    private Integer stock;
}
