package com.steam.controller;

import com.steam.dto.Result;
import com.steam.entity.UserPreorder;
import com.steam.service.PreorderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/preorders")
@RequiredArgsConstructor
public class PreorderController {

    private final PreorderService preorderService;

    @GetMapping
    public Result<List<UserPreorder>> getMyPreorders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<UserPreorder> list = preorderService.getUserPreorders(userId);
        return Result.success(list);
    }

    @GetMapping("/count")
    public Result<Integer> getPreorderCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int count = preorderService.getPreorderCount(userId);
        return Result.success(count);
    }

    @GetMapping("/check/{gameId}")
    public Result<Boolean> checkPreordered(HttpServletRequest request, @PathVariable Long gameId) {
        Long userId = (Long) request.getAttribute("userId");
        boolean has = preorderService.hasPreordered(userId, gameId);
        return Result.success(has);
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> getPreorderSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> summary = new HashMap<>();
        summary.put("count", preorderService.getPreorderCount(userId));
        summary.put("list", preorderService.getUserPreorders(userId));
        return Result.success(summary);
    }
}
