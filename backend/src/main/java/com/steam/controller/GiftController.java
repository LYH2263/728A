package com.steam.controller;

import com.steam.dto.Result;
import com.steam.entity.Gift;
import com.steam.service.GiftService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gifts")
@RequiredArgsConstructor
public class GiftController {

    private final GiftService giftService;

    @GetMapping("/received")
    public Result<List<Gift>> getReceivedGifts(HttpServletRequest request,
                                               @RequestParam(required = false) String status) {
        Long userId = (Long) request.getAttribute("userId");
        List<Gift> gifts = giftService.getReceivedGifts(userId, status);
        return Result.success(gifts);
    }

    @GetMapping("/sent")
    public Result<List<Gift>> getSentGifts(HttpServletRequest request,
                                           @RequestParam(required = false) String status) {
        Long userId = (Long) request.getAttribute("userId");
        List<Gift> gifts = giftService.getSentGifts(userId, status);
        return Result.success(gifts);
    }

    @GetMapping("/pending/count")
    public Result<Integer> getPendingCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int count = giftService.countPendingGifts(userId);
        return Result.success(count);
    }

    @GetMapping("/{giftId}")
    public Result<Gift> getGiftDetail(HttpServletRequest request, @PathVariable Long giftId) {
        Long userId = (Long) request.getAttribute("userId");
        Gift gift = giftService.getGiftDetail(userId, giftId);
        return Result.success(gift);
    }

    @PostMapping("/{giftId}/claim")
    public Result<Gift> claimGift(HttpServletRequest request, @PathVariable Long giftId) {
        Long userId = (Long) request.getAttribute("userId");
        Gift gift = giftService.claimGift(userId, giftId);
        return Result.success("礼物领取成功", gift);
    }

    @PostMapping("/{giftId}/reject")
    public Result<Gift> rejectGift(HttpServletRequest request, @PathVariable Long giftId) {
        Long userId = (Long) request.getAttribute("userId");
        Gift gift = giftService.rejectGift(userId, giftId);
        return Result.success("已拒收礼物，款项已退还给赠送者", gift);
    }
}
