package com.steam.controller;

import com.steam.dto.PageResult;
import com.steam.dto.Result;
import com.steam.entity.WalletTransaction;
import com.steam.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> overview = walletService.getWalletOverview(userId);
        return Result.success(overview);
    }

    @GetMapping("/transactions")
    public Result<PageResult<WalletTransaction>> getTransactions(
            HttpServletRequest request,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<WalletTransaction> result = walletService.getTransactions(userId, type, month, page, size);
        return Result.success(result);
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> getMonthlySummary(
            HttpServletRequest request,
            @RequestParam(required = false) String month) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> summary = walletService.getMonthlySummary(userId, month);
        return Result.success(summary);
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrendData(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> trendData = walletService.getTrendData(userId);
        return Result.success(trendData);
    }
}
