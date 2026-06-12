package com.steam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplyRefundDTO {
    @NotNull(message = "订单项ID不能为空")
    private Long orderItemId;

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 500, message = "退款原因不能超过500个字符")
    private String reason;
}
