package com.steam.mapper;

import com.steam.entity.Friendship;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendshipMapper {

    @Select("SELECT * FROM friendships WHERE user_id = #{userId} AND friend_id = #{friendId}")
    Friendship findByUserIdAndFriendId(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Select("SELECT f.*, u.id as user_id, u.username, u.nickname, u.avatar, u.status " +
            "FROM friendships f " +
            "INNER JOIN users u ON f.friend_id = u.id " +
            "WHERE f.user_id = #{userId} AND f.status = #{status} " +
            "ORDER BY f.updated_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "friendId", column = "friend_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "actionUserId", column = "action_user_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "friendUser.id", column = "user_id"),
            @Result(property = "friendUser.username", column = "username"),
            @Result(property = "friendUser.nickname", column = "nickname"),
            @Result(property = "friendUser.avatar", column = "avatar"),
            @Result(property = "friendUser.status", column = "status")
    })
    List<Friendship> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Select("SELECT f.*, u.id as user_id, u.username, u.nickname, u.avatar, u.status " +
            "FROM friendships f " +
            "INNER JOIN users u ON f.user_id = u.id " +
            "WHERE f.friend_id = #{userId} AND f.status = 'PENDING' " +
            "ORDER BY f.created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "friendId", column = "friend_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "actionUserId", column = "action_user_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "friendUser.id", column = "user_id"),
            @Result(property = "friendUser.username", column = "username"),
            @Result(property = "friendUser.nickname", column = "nickname"),
            @Result(property = "friendUser.avatar", column = "avatar"),
            @Result(property = "friendUser.status", column = "status")
    })
    List<Friendship> findPendingRequests(Long userId);

    @Select("SELECT COUNT(*) FROM friendships WHERE friend_id = #{userId} AND status = 'PENDING'")
    Long countPendingRequests(Long userId);

    @Insert("INSERT INTO friendships (user_id, friend_id, status, action_user_id) " +
            "VALUES (#{userId}, #{friendId}, #{status}, #{actionUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Friendship friendship);

    @Update("UPDATE friendships SET status = #{status}, action_user_id = #{actionUserId}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int updateStatus(@Param("userId") Long userId, @Param("friendId") Long friendId,
                     @Param("status") String status, @Param("actionUserId") Long actionUserId);

    @Delete("DELETE FROM friendships WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int delete(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Select("SELECT friend_id FROM friendships WHERE user_id = #{userId} AND status = 'ACCEPTED'")
    List<Long> findFriendIdsByUserId(Long userId);
}
