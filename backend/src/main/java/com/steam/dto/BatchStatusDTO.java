package com.steam.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchStatusDTO {
    private List<Long> gameIds;
    private Integer status;
}
