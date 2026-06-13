package com.steam.controller;

import com.steam.dto.CouponApplyDTO;
import com.steam.dto.Result;
import com.steam.entity.Coupon;
import com.steam.entity.UserCoupon;
import com.steam.service.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/claim")
    public Result<UserCoupon> claimCoupon(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String code = body.get("code");
        UserCoupon userCoupon = couponService.claimCoupon(userId, code);
        return Result.success("领取成功", userCoupon);
    }

    @GetMapping("/my")
    public Result<List<UserCoupon>> getMyCoupons(HttpServletRequest request,
                                                  @RequestParam(required = false) String status) {
        Long userId = (Long) request.getAttribute("userId");
        List<UserCoupon> coupons = couponService.getUserCoupons(userId, status);
        return Result.success(coupons);
    }

    @GetMapping("/available")
    public Result<List<UserCoupon>> getAvailableCoupons(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<UserCoupon> coupons = couponService.getAvailableCoupons(userId);
        return Result.success(coupons);
    }

    @PostMapping("/applicable")
    public Result<List<UserCoupon>> getApplicableCoupons(HttpServletRequest request,
                                                          @RequestBody CouponApplyDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        List<Long> gameIds = dto.getGameIds();
        if (gameIds == null || gameIds.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }
        List<UserCoupon> coupons = couponService.getApplicableCoupons(userId, gameIds);
        return Result.success(coupons);
    }

    @GetMapping("/{code}")
    public Result<Coupon> getCouponByCode(@PathVariable String code) {
        Coupon coupon = couponService.getCouponByCode(code);
        return Result.success(coupon);
    }
}
