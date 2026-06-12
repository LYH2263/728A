package com.steam.mapper;

import com.steam.entity.Coupon;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CouponMapper {

    @Select("SELECT * FROM coupons WHERE id = #{id}")
    Coupon findById(Long id);

    @Select("SELECT * FROM coupons WHERE code = #{code}")
    Coupon findByCode(String code);

    @Select("SELECT * FROM coupons WHERE status = 1 ORDER BY created_at DESC")
    List<Coupon> findAllAvailable();

    @Select("SELECT * FROM coupons WHERE status = 1 AND type = #{type} ORDER BY created_at DESC")
    List<Coupon> findByType(String type);

    @Update("UPDATE coupons SET claimed_count = claimed_count + 1 WHERE id = #{id} AND (total_count = -1 OR claimed_count < total_count)")
    int increaseClaimedCount(Long id);
}
