package com.steam.mapper;

import com.steam.entity.UserPreorder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserPreorderMapper {

    @Insert("INSERT INTO user_preorders (user_id, game_id, order_id, order_item_id, price_paid, release_status, status) " +
            "VALUES (#{userId}, #{gameId}, #{orderId}, #{orderItemId}, #{pricePaid}, #{releaseStatus}, 'PENDING_RELEASE')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserPreorder preorder);

    @Select("SELECT up.*, g.id as game_id, g.title, g.cover_image, g.developer, g.release_date, " +
            "g.release_status, g.preorder_unlock_date, g.crowdfunding_goal, g.current_funding, g.supporter_count " +
            "FROM user_preorders up " +
            "INNER JOIN games g ON up.game_id = g.id " +
            "WHERE up.user_id = #{userId} " +
            "ORDER BY up.created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "gameId", column = "game_id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "orderItemId", column = "order_item_id"),
            @Result(property = "pricePaid", column = "price_paid"),
            @Result(property = "releaseStatus", column = "release_status"),
            @Result(property = "status", column = "status"),
            @Result(property = "convertedAt", column = "converted_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "game.id", column = "game_id"),
            @Result(property = "game.title", column = "title"),
            @Result(property = "game.coverImage", column = "cover_image"),
            @Result(property = "game.developer", column = "developer"),
            @Result(property = "game.releaseDate", column = "release_date"),
            @Result(property = "game.releaseStatus", column = "release_status"),
            @Result(property = "game.preorderUnlockDate", column = "preorder_unlock_date"),
            @Result(property = "game.crowdfundingGoal", column = "crowdfunding_goal"),
            @Result(property = "game.currentFunding", column = "current_funding"),
            @Result(property = "game.supporterCount", column = "supporter_count")
    })
    List<UserPreorder> findByUserId(Long userId);

    @Select("SELECT * FROM user_preorders WHERE user_id = #{userId} AND game_id = #{gameId} AND status = 'PENDING_RELEASE'")
    UserPreorder findPendingByUserIdAndGameId(@Param("userId") Long userId, @Param("gameId") Long gameId);

    @Select("SELECT COUNT(*) FROM user_preorders WHERE user_id = #{userId} AND status = 'PENDING_RELEASE'")
    int countPendingByUserId(Long userId);

    @Select("SELECT EXISTS(SELECT 1 FROM user_preorders WHERE user_id = #{userId} AND game_id = #{gameId} AND status = 'PENDING_RELEASE')")
    boolean existsPendingByUserIdAndGameId(@Param("userId") Long userId, @Param("gameId") Long gameId);

    List<UserPreorder> findReadyToConvert(@Param("now") java.time.LocalDateTime now);

    @Update("UPDATE user_preorders SET status = 'RELEASED', converted_at = #{convertedAt} WHERE id = #{id}")
    int markAsReleased(@Param("id") Long id, @Param("convertedAt") java.time.LocalDateTime convertedAt);
}
