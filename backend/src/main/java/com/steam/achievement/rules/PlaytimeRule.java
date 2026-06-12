package com.steam.achievement.rules;

import com.steam.achievement.AchievementEvent;
import com.steam.achievement.AchievementRule;
import com.steam.entity.Achievement;
import com.steam.mapper.UserLibraryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("PLAYTIME_UPDATED")
@RequiredArgsConstructor
public class PlaytimeRule implements AchievementRule {

    private final UserLibraryMapper userLibraryMapper;

    @Override
    public boolean matches(Achievement achievement, AchievementEvent event) {
        return AchievementEvent.PLAYTIME_UPDATED.equals(event.getEventType());
    }

    @Override
    public int calculateProgress(Achievement achievement, AchievementEvent event, int currentProgress) {
        Long userId = event.getUserId();
        try {
            Integer totalPlaytime = userLibraryMapper.getTotalPlaytimeByUserId(userId);
            return totalPlaytime != null ? totalPlaytime : 0;
        } catch (Exception e) {
            log.error("计算游玩时长类成就进度失败: userId={}, achievementId={}", userId, achievement.getId(), e);
            return currentProgress;
        }
    }
}
