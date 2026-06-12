package com.steam.mapper;

import com.steam.entity.Achievement;
import com.steam.entity.UserAchievement;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AchievementMapper {

    @Select("SELECT * FROM achievements WHERE status = 1 AND event_type = #{eventType} ORDER BY sort_order")
    List<Achievement> findByEventType(String eventType);

    @Select("SELECT * FROM achievements WHERE status = 1 ORDER BY sort_order")
    List<Achievement> findAllActive();

    @Select("SELECT * FROM achievements WHERE id = #{id}")
    Achievement findById(Long id);

    @Select("SELECT * FROM achievements WHERE code = #{code}")
    Achievement findByCode(String code);

    @Insert("INSERT INTO user_achievements (user_id, achievement_id, progress, target_value, is_unlocked, unlocked_at) " +
            "VALUES (#{userId}, #{achievementId}, #{progress}, #{targetValue}, #{isUnlocked}, #{unlockedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUserAchievement(UserAchievement userAchievement);

    @Update("UPDATE user_achievements SET progress = #{progress}, target_value = #{targetValue}, " +
            "is_unlocked = #{isUnlocked}, unlocked_at = #{unlockedAt} WHERE id = #{id}")
    int updateUserAchievement(UserAchievement userAchievement);

    @Select("SELECT ua.*, a.id as ach_id, a.code, a.name, a.description, a.icon, a.category, " +
            "a.target_value as ach_target, a.is_progress, a.rarity, a.points, a.event_type, a.rule_config " +
            "FROM user_achievements ua " +
            "INNER JOIN achievements a ON ua.achievement_id = a.id " +
            "WHERE ua.user_id = #{userId} " +
            "ORDER BY ua.is_unlocked DESC, ua.updated_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "achievementId", column = "achievement_id"),
            @Result(property = "progress", column = "progress"),
            @Result(property = "targetValue", column = "target_value"),
            @Result(property = "isUnlocked", column = "is_unlocked"),
            @Result(property = "unlockedAt", column = "unlocked_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "achievement.id", column = "ach_id"),
            @Result(property = "achievement.code", column = "code"),
            @Result(property = "achievement.name", column = "name"),
            @Result(property = "achievement.description", column = "description"),
            @Result(property = "achievement.icon", column = "icon"),
            @Result(property = "achievement.category", column = "category"),
            @Result(property = "achievement.targetValue", column = "ach_target"),
            @Result(property = "achievement.isProgress", column = "is_progress"),
            @Result(property = "achievement.rarity", column = "rarity"),
            @Result(property = "achievement.points", column = "points"),
            @Result(property = "achievement.eventType", column = "event_type"),
            @Result(property = "achievement.ruleConfig", column = "rule_config")
    })
    List<UserAchievement> findUserAchievements(Long userId);

    @Select("SELECT * FROM user_achievements WHERE user_id = #{userId} AND achievement_id = #{achievementId}")
    UserAchievement findUserAchievement(@Param("userId") Long userId, @Param("achievementId") Long achievementId);

    @Select("SELECT ua.*, a.id as ach_id, a.code, a.name, a.description, a.icon, a.category, " +
            "a.target_value as ach_target, a.is_progress, a.rarity, a.points, a.event_type, a.rule_config " +
            "FROM user_achievements ua " +
            "INNER JOIN achievements a ON ua.achievement_id = a.id " +
            "WHERE ua.user_id = #{userId} AND ua.is_unlocked = 1 " +
            "ORDER BY ua.unlocked_at DESC " +
            "LIMIT #{limit}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "achievementId", column = "achievement_id"),
            @Result(property = "progress", column = "progress"),
            @Result(property = "targetValue", column = "target_value"),
            @Result(property = "isUnlocked", column = "is_unlocked"),
            @Result(property = "unlockedAt", column = "unlocked_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "achievement.id", column = "ach_id"),
            @Result(property = "achievement.code", column = "code"),
            @Result(property = "achievement.name", column = "name"),
            @Result(property = "achievement.description", column = "description"),
            @Result(property = "achievement.icon", column = "icon"),
            @Result(property = "achievement.category", column = "category"),
            @Result(property = "achievement.targetValue", column = "ach_target"),
            @Result(property = "achievement.isProgress", column = "is_progress"),
            @Result(property = "achievement.rarity", column = "rarity"),
            @Result(property = "achievement.points", column = "points"),
            @Result(property = "achievement.eventType", column = "event_type"),
            @Result(property = "achievement.ruleConfig", column = "rule_config")
    })
    List<UserAchievement> findRecentUnlocked(@Param("userId") Long userId, @Param("limit") Integer limit);
}
