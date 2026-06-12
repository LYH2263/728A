package com.steam.achievement;

import com.steam.achievement.rules.*;
import com.steam.entity.Achievement;
import com.steam.entity.UserAchievement;
import com.steam.mapper.AchievementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementEngine {

    private final AchievementMapper achievementMapper;
    private final OrderRule orderRule;
    private final ReviewRule reviewRule;
    private final PlaytimeRule playtimeRule;
    private final LibraryRule libraryRule;

    private AchievementRule getRule(String eventType) {
        return switch (eventType) {
            case AchievementEvent.ORDER_PAID -> orderRule;
            case AchievementEvent.REVIEW_CREATED -> reviewRule;
            case AchievementEvent.PLAYTIME_UPDATED -> playtimeRule;
            case AchievementEvent.LIBRARY_UPDATED -> libraryRule;
            default -> null;
        };
    }

    @Transactional
    public List<UserAchievement> processEvent(AchievementEvent event) {
        String eventType = event.getEventType();
        Long userId = event.getUserId();
        log.info("处理成就事件: eventType={}, userId={}", eventType, userId);

        AchievementRule rule = getRule(eventType);
        if (rule == null) {
            log.warn("未找到对应的成就规则: eventType={}", eventType);
            return Collections.emptyList();
        }

        List<Achievement> achievements = achievementMapper.findByEventType(eventType);
        if (achievements == null || achievements.isEmpty()) {
            log.info("没有配置该事件类型的成就: eventType={}", eventType);
            return Collections.emptyList();
        }

        List<UserAchievement> newlyUnlocked = new ArrayList<>();

        for (Achievement achievement : achievements) {
            try {
                if (!rule.matches(achievement, event)) {
                    continue;
                }

                UserAchievement userAch = achievementMapper.findUserAchievement(userId, achievement.getId());
                int currentProgress = userAch != null ? userAch.getProgress() : 0;
                int targetValue = achievement.getTargetValue();

                int newProgress = rule.calculateProgress(achievement, event, currentProgress);
                boolean wasUnlocked = userAch != null && userAch.getIsUnlocked() == 1;
                boolean nowUnlocked = newProgress >= targetValue;

                if (userAch == null) {
                    userAch = new UserAchievement();
                    userAch.setUserId(userId);
                    userAch.setAchievementId(achievement.getId());
                    userAch.setProgress(Math.min(newProgress, targetValue));
                    userAch.setTargetValue(targetValue);
                    userAch.setIsUnlocked(nowUnlocked ? 1 : 0);
                    userAch.setUnlockedAt(nowUnlocked ? LocalDateTime.now() : null);
                    achievementMapper.insertUserAchievement(userAch);
                    log.info("创建用户成就记录: userId={}, achievementId={}, progress={}/{}",
                            userId, achievement.getId(), userAch.getProgress(), targetValue);
                } else {
                    boolean progressChanged = newProgress != currentProgress;
                    if (progressChanged || (nowUnlocked && !wasUnlocked)) {
                        userAch.setProgress(Math.min(newProgress, targetValue));
                        userAch.setTargetValue(targetValue);
                        if (nowUnlocked && !wasUnlocked) {
                            userAch.setIsUnlocked(1);
                            userAch.setUnlockedAt(LocalDateTime.now());
                        }
                        achievementMapper.updateUserAchievement(userAch);
                        log.info("更新用户成就记录: userId={}, achievementId={}, progress={}/{}, unlocked={}",
                                userId, achievement.getId(), userAch.getProgress(), targetValue, userAch.getIsUnlocked() == 1);
                    }
                }

                if (nowUnlocked && !wasUnlocked) {
                    userAch.setAchievement(achievement);
                    newlyUnlocked.add(userAch);
                    log.info("成就解锁: userId={}, achievement={}, code={}",
                            userId, achievement.getName(), achievement.getCode());
                }
            } catch (Exception e) {
                log.error("处理成就异常: achievementId={}, userId={}", achievement.getId(), userId, e);
            }
        }

        return newlyUnlocked;
    }
}
