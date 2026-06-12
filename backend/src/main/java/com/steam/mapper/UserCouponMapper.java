package com.steam.mapper;

import com.steam.entity.UserCoupon;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserCouponMapper {

    @Select("SELECT * FROM user_coupons WHERE id = #{id}")
    UserCoupon findById(Long id);

    @Select("SELECT * FROM user_coupons WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<UserCoupon> findByUserId(Long userId);

    @Select("SELECT * FROM user_coupons WHERE user_id = #{userId} AND status = #{status} ORDER BY created_at DESC")
    List<UserCoupon> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM user_coupons WHERE user_id = #{userId} AND coupon_id = #{couponId}")
    int countByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @Select("SELECT * FROM user_coupons WHERE user_id = #{userId} AND coupon_id = #{couponId} LIMIT 1")
    UserCoupon findByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @Insert("INSERT INTO user_coupons (user_id, coupon_id, status) " +
            "VALUES (#{userId}, #{couponId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserCoupon userCoupon);

    @Update("UPDATE user_coupons SET status = 'USED', order_id = #{orderId}, used_at = #{usedAt}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} AND status = 'UNUSED'")
    int markAsUsed(@Param("id") Long id, @Param("orderId") Long orderId,
                   @Param("usedAt") java.time.LocalDateTime usedAt);

    @Update("UPDATE user_coupons SET status = 'UNUSED', order_id = NULL, used_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int resetStatus(Long id);

    @Select("SELECT uc.* FROM user_coupons uc " +
            "INNER JOIN coupons c ON uc.coupon_id = c.id " +
            "WHERE uc.user_id = #{userId} AND uc.status = 'UNUSED' " +
            "AND c.valid_start <= NOW() AND c.valid_end > NOW() " +
            "AND c.status = 1")
    List<UserCoupon> findAvailableByUserId(Long userId);

    @Select("SELECT * FROM user_coupons WHERE order_id = #{orderId} AND status = 'USED' LIMIT 1")
    UserCoupon findByOrderId(Long orderId);
}
