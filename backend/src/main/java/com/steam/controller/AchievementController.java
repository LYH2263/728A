package com.steam.controller;

import com.steam.dto.Result;
import com.steam.entity.UserAchievement;
import com.steam.service.AchievementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping("/my")
    public Result<List<UserAchievement>> getMyAchievements(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<UserAchievement> list = achievementService.getUserAchievements(userId);
        return Result.success(list);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getAchievementStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> stats = achievementService.getAchievementStats(userId);
        return Result.success(stats);
    }

    @GetMapping("/recent")
    public Result<List<UserAchievement>> getRecentUnlocked(
            HttpServletRequest request,
            @RequestParam(defaultValue = "5") Integer limit) {
        Long userId = (Long) request.getAttribute("userId");
        List<UserAchievement> list = achievementService.getRecentUnlocked(userId, limit);
        return Result.success(list);
    }
}
