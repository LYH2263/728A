package com.steam.controller;

import com.steam.dto.ApplyRefundDTO;
import com.steam.dto.RefundReviewDTO;
import com.steam.dto.Result;
import com.steam.entity.RefundRequest;
import com.steam.service.RefundService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/apply")
    public Result<RefundRequest> applyRefund(HttpServletRequest request, @Valid @RequestBody ApplyRefundDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        RefundRequest refund = refundService.applyRefund(userId, dto.getOrderItemId(), dto.getReason());
        return Result.success("退款申请已提交", refund);
    }

    @GetMapping("/check/{orderItemId}")
    public Result<Map<String, Object>> checkEligibility(HttpServletRequest request, @PathVariable Long orderItemId) {
        Long userId = (Long) request.getAttribute("userId");
        boolean eligible = refundService.isRefundEligible(userId, orderItemId);
        String refundStatus = refundService.getItemRefundStatus(orderItemId);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("eligible", eligible);
        data.put("refundStatus", refundStatus);
        return Result.success(data);
    }

    @GetMapping("/my")
    public Result<List<RefundRequest>> getMyRefunds(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<RefundRequest> refunds = refundService.getUserRefunds(userId);
        return Result.success(refunds);
    }

    @GetMapping("/{id}")
    public Result<RefundRequest> getRefundDetail(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        RefundRequest refund;
        if ("ADMIN".equals(role)) {
            refund = refundService.getRefundDetailAdmin(id);
        } else {
            refund = refundService.getRefundDetail(userId, id);
        }
        return Result.success(refund);
    }

    @GetMapping("/admin/list")
    public Result<List<RefundRequest>> getAllRefunds(HttpServletRequest request,
                                                      @RequestParam(required = false) String status) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权访问");
        }
        List<RefundRequest> refunds = refundService.getAllRefunds(status);
        return Result.success(refunds);
    }

    @PostMapping("/admin/review")
    public Result<RefundRequest> reviewRefund(HttpServletRequest request, @Valid @RequestBody RefundReviewDTO dto) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权访问");
        }
        Long adminUserId = (Long) request.getAttribute("userId");
        RefundRequest refund = refundService.reviewRefund(adminUserId, dto.getRefundId(), dto.getAction(), dto.getRemark());
        return Result.success("审核完成", refund);
    }
}
