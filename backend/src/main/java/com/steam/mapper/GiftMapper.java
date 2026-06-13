package com.steam.mapper;

import com.steam.entity.Gift;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface GiftMapper {

    @Insert("INSERT INTO gifts (gift_no, sender_id, recipient_id, game_id, game_title, game_cover, " +
            "order_id, order_item_id, price_paid, status, message) " +
            "VALUES (#{giftNo}, #{senderId}, #{recipientId}, #{gameId}, #{gameTitle}, #{gameCover}, " +
            "#{orderId}, #{orderItemId}, #{pricePaid}, #{status}, #{message})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Gift gift);

    @Select("SELECT * FROM gifts WHERE id = #{id}")
    Gift findById(Long id);

    @Select("SELECT * FROM gifts WHERE gift_no = #{giftNo}")
    Gift findByGiftNo(String giftNo);

    @Select("SELECT * FROM gifts WHERE recipient_id = #{recipientId} ORDER BY created_at DESC")
    List<Gift> findByRecipientId(Long recipientId);

    @Select("SELECT * FROM gifts WHERE sender_id = #{senderId} ORDER BY created_at DESC")
    List<Gift> findBySenderId(Long senderId);

    @Select("SELECT * FROM gifts WHERE recipient_id = #{recipientId} AND status = #{status} ORDER BY created_at DESC")
    List<Gift> findByRecipientIdAndStatus(@Param("recipientId") Long recipientId, @Param("status") String status);

    @Select("SELECT * FROM gifts WHERE sender_id = #{senderId} AND status = #{status} ORDER BY created_at DESC")
    List<Gift> findBySenderIdAndStatus(@Param("senderId") Long senderId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM gifts WHERE recipient_id = #{recipientId} AND status = 'PENDING'")
    int countPendingByRecipientId(Long recipientId);

    @Update("UPDATE gifts SET status = 'CLAIMED', claimed_at = #{claimedAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateToClaimed(@Param("id") Long id, @Param("claimedAt") LocalDateTime claimedAt);

    @Update("UPDATE gifts SET status = 'REJECTED', rejected_at = #{rejectedAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateToRejected(@Param("id") Long id, @Param("rejectedAt") LocalDateTime rejectedAt);

    @Select("SELECT COUNT(*) > 0 FROM gifts WHERE recipient_id = #{recipientId} AND game_id = #{gameId} AND status = 'PENDING'")
    boolean existsPendingByRecipientAndGame(@Param("recipientId") Long recipientId, @Param("gameId") Long gameId);
}
