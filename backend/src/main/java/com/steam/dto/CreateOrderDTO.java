package com.steam.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CreateOrderDTO {
    @NotEmpty(message = "购买的游戏不能为空")
    private List<Long> gameIds;
    private Long userCouponId;
    private Long recipientId;
    private String giftMessage;
}
