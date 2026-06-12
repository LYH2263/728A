package com.steam.dto;

import lombok.Data;

@Data
public class AdminGameQueryDTO {
    private String keyword;
    private Integer status;
    private Boolean lowStockOnly;
    private String sortBy;
    private String sortOrder;
    private Integer page = 1;
    private Integer size = 10;
}
