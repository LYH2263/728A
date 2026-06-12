package com.steam.mapper;

import com.steam.entity.Activity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ActivityMapper {

    @Insert("INSERT INTO activities (user_id, type, game_id, game_title, game_cover, " +
            "achievement_id, achievement_name, review_id, review_rating, review_content, metadata) " +
            "VALUES (#{userId}, #{type}, #{gameId}, #{gameTitle}, #{gameCover}, " +
            "#{achievementId}, #{achievementName}, #{reviewId}, #{reviewRating}, #{reviewContent}, #{metadata})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Activity activity);

    @Select("SELECT a.*, u.id as user_id, u.username, u.nickname, u.avatar " +
            "FROM activities a " +
            "INNER JOIN users u ON a.user_id = u.id " +
            "WHERE a.user_id IN " +
            "(SELECT friend_id FROM friendships WHERE user_id = #{userId} AND status = 'ACCEPTED') " +
            "ORDER BY a.created_at DESC " +
            "LIMIT #{offset}, #{size}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "gameId", column = "game_id"),
            @Result(property = "gameTitle", column = "game_title"),
            @Result(property = "gameCover", column = "game_cover"),
            @Result(property = "achievementId", column = "achievement_id"),
            @Result(property = "achievementName", column = "achievement_name"),
            @Result(property = "reviewId", column = "review_id"),
            @Result(property = "reviewRating", column = "review_rating"),
            @Result(property = "reviewContent", column = "review_content"),
            @Result(property = "metadata", column = "metadata"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "user.id", column = "user_id"),
            @Result(property = "user.username", column = "username"),
            @Result(property = "user.nickname", column = "nickname"),
            @Result(property = "user.avatar", column = "avatar")
    })
    List<Activity> findFriendActivities(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM activities a " +
            "WHERE a.user_id IN " +
            "(SELECT friend_id FROM friendships WHERE user_id = #{userId} AND status = 'ACCEPTED')")
    Long countFriendActivities(Long userId);

    @Select("SELECT a.*, u.id as user_id, u.username, u.nickname, u.avatar " +
            "FROM activities a " +
            "INNER JOIN users u ON a.user_id = u.id " +
            "WHERE a.user_id = #{userId} " +
            "ORDER BY a.created_at DESC " +
            "LIMIT #{offset}, #{size}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "gameId", column = "game_id"),
            @Result(property = "gameTitle", column = "game_title"),
            @Result(property = "gameCover", column = "game_cover"),
            @Result(property = "achievementId", column = "achievement_id"),
            @Result(property = "achievementName", column = "achievement_name"),
            @Result(property = "reviewId", column = "review_id"),
            @Result(property = "reviewRating", column = "review_rating"),
            @Result(property = "reviewContent", column = "review_content"),
            @Result(property = "metadata", column = "metadata"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "user.id", column = "user_id"),
            @Result(property = "user.username", column = "username"),
            @Result(property = "user.nickname", column = "nickname"),
            @Result(property = "user.avatar", column = "avatar")
    })
    List<Activity> findUserActivities(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM activities WHERE user_id = #{userId}")
    Long countUserActivities(Long userId);
}
