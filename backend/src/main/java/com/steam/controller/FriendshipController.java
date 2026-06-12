package com.steam.controller;

import com.steam.dto.PageResult;
import com.steam.dto.Result;
import com.steam.entity.Friendship;
import com.steam.entity.User;
import com.steam.service.FriendshipService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @GetMapping("/list")
    public Result<List<Friendship>> getFriends(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Friendship> friends = friendshipService.getFriends(userId);
        return Result.success(friends);
    }

    @GetMapping("/pending")
    public Result<List<Friendship>> getPendingRequests(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Friendship> requests = friendshipService.getPendingRequests(userId);
        return Result.success(requests);
    }

    @GetMapping("/pending/count")
    public Result<Long> getPendingCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long count = friendshipService.getPendingRequestCount(userId);
        return Result.success(count);
    }

    @GetMapping("/blocked")
    public Result<List<Friendship>> getBlockedUsers(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Friendship> blocked = friendshipService.getBlockedUsers(userId);
        return Result.success(blocked);
    }

    @GetMapping("/search")
    public Result<PageResult<User>> searchUsers(HttpServletRequest request,
                                              @RequestParam String keyword,
                                              @RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<User> result = friendshipService.searchUsers(userId, keyword, page, size);
        return Result.success(result);
    }

    @PostMapping("/request/{friendId}")
    public Result<Void> sendFriendRequest(HttpServletRequest request, @PathVariable Long friendId) {
        Long userId = (Long) request.getAttribute("userId");
        friendshipService.sendFriendRequest(userId, friendId);
        return Result.successMessage("好友请求已发送");
    }

    @PostMapping("/accept/{friendId}")
    public Result<Void> acceptFriendRequest(HttpServletRequest request, @PathVariable Long friendId) {
        Long userId = (Long) request.getAttribute("userId");
        friendshipService.acceptFriendRequest(userId, friendId);
        return Result.successMessage("已接受好友请求");
    }

    @PostMapping("/reject/{friendId}")
    public Result<Void> rejectFriendRequest(HttpServletRequest request, @PathVariable Long friendId) {
        Long userId = (Long) request.getAttribute("userId");
        friendshipService.rejectFriendRequest(userId, friendId);
        return Result.successMessage("已拒绝好友请求");
    }

    @DeleteMapping("/{friendId}")
    public Result<Void> deleteFriend(HttpServletRequest request, @PathVariable Long friendId) {
        Long userId = (Long) request.getAttribute("userId");
        friendshipService.deleteFriend(userId, friendId);
        return Result.successMessage("已删除好友");
    }

    @PostMapping("/block/{friendId}")
    public Result<Void> blockUser(HttpServletRequest request, @PathVariable Long friendId) {
        Long userId = (Long) request.getAttribute("userId");
        friendshipService.blockUser(userId, friendId);
        return Result.successMessage("已拉黑用户");
    }

    @PostMapping("/unblock/{friendId}")
    public Result<Void> unblockUser(HttpServletRequest request, @PathVariable Long friendId) {
        Long userId = (Long) request.getAttribute("userId");
        friendshipService.unblockUser(userId, friendId);
        return Result.successMessage("已取消拉黑");
    }

    @GetMapping("/status/{targetUserId}")
    public Result<Friendship> getFriendshipStatus(HttpServletRequest request, @PathVariable Long targetUserId) {
        Long userId = (Long) request.getAttribute("userId");
        Friendship friendship = friendshipService.getFriendshipStatus(userId, targetUserId);
        return Result.success(friendship);
    }
}
