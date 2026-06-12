package com.steam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundReviewDTO {
    @NotNull(message = "退款申请ID不能为空")
    private Long refundId;

    @NotBlank(message = "审核结果不能为空")
    private String action;

    @Size(max = 500, message = "审核备注不能超过500个字符")
    private String remark;
}
