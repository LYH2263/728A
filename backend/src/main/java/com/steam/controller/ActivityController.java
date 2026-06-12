package com.steam.controller;

import com.steam.dto.PageResult;
import com.steam.dto.Result;
import com.steam.entity.Activity;
import com.steam.service.ActivityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/friends")
    public Result<PageResult<Activity>> getFriendActivities(HttpServletRequest request,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer size) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Activity> result = activityService.getFriendActivities(userId, page, size);
        return Result.success(result);
    }

    @GetMapping("/mine")
    public Result<PageResult<Activity>> getMyActivities(HttpServletRequest request,
                                                        @RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Activity> result = activityService.getUserActivities(userId, page, size);
        return Result.success(result);
    }
}
