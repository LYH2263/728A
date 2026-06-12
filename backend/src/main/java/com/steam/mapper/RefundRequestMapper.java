package com.steam.mapper;

import com.steam.entity.RefundRequest;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RefundRequestMapper {

    @Select("SELECT * FROM refund_requests WHERE id = #{id}")
    RefundRequest findById(Long id);

    @Select("SELECT * FROM refund_requests WHERE refund_no = #{refundNo}")
    RefundRequest findByRefundNo(String refundNo);

    @Select("SELECT * FROM refund_requests WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<RefundRequest> findByUserId(Long userId);

    @Select("SELECT * FROM refund_requests WHERE order_id = #{orderId} ORDER BY created_at DESC")
    List<RefundRequest> findByOrderId(Long orderId);

    @Select("SELECT * FROM refund_requests WHERE order_item_id = #{orderItemId} AND status IN ('PENDING', 'APPROVED', 'REFUNDED')")
    RefundRequest findActiveByOrderItemId(Long orderItemId);

    @Select("SELECT * FROM refund_requests WHERE status = #{status} ORDER BY created_at DESC")
    List<RefundRequest> findByStatus(String status);

    @Select("SELECT * FROM refund_requests ORDER BY created_at DESC")
    List<RefundRequest> findAll();

    @Insert("INSERT INTO refund_requests (refund_no, order_id, order_no, user_id, game_id, game_title, game_cover, " +
            "order_item_id, order_item_price, reason, status) " +
            "VALUES (#{refundNo}, #{orderId}, #{orderNo}, #{userId}, #{gameId}, #{gameTitle}, #{gameCover}, " +
            "#{orderItemId}, #{orderItemPrice}, #{reason}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RefundRequest refundRequest);

    @Update("UPDATE refund_requests SET status = #{status}, review_user_id = #{reviewUserId}, " +
            "review_remark = #{reviewRemark}, reviewed_at = #{reviewedAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateReview(@Param("id") Long id, @Param("status") String status,
                     @Param("reviewUserId") Long reviewUserId, @Param("reviewRemark") String reviewRemark,
                     @Param("reviewedAt") LocalDateTime reviewedAt);

    @Update("UPDATE refund_requests SET status = 'REFUNDED', refunded_at = #{refundedAt}, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int markRefunded(@Param("id") Long id, @Param("refundedAt") LocalDateTime refundedAt);

    @Select("SELECT COALESCE(SUM(order_item_price), 0) FROM refund_requests " +
            "WHERE order_id = #{orderId} AND status IN ('APPROVED', 'REFUNDED')")
    BigDecimal sumRefundedAmountByOrderId(Long orderId);

    @Select("SELECT COUNT(*) FROM refund_requests WHERE order_item_id = #{orderItemId} AND status IN ('PENDING', 'APPROVED', 'REFUNDED')")
    int countActiveByOrderItemId(Long orderItemId);
}
