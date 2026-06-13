package com.steam.dto;

import lombok.Data;
import java.util.List;

@Data
public class CouponApplyDTO {
    private List<Long> gameIds;
}
