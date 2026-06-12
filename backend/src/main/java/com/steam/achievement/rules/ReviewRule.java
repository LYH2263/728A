package com.steam.achievement.rules;

import com.steam.achievement.AchievementEvent;
import com.steam.achievement.AchievementRule;
import com.steam.entity.Achievement;
import com.steam.mapper.GameReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("REVIEW_CREATED")
@RequiredArgsConstructor
public class ReviewRule implements AchievementRule {

    private final GameReviewMapper reviewMapper;

    @Override
    public boolean matches(Achievement achievement, AchievementEvent event) {
        return AchievementEvent.REVIEW_CREATED.equals(event.getEventType());
    }

    @Override
    public int calculateProgress(Achievement achievement, AchievementEvent event, int currentProgress) {
        Long userId = event.getUserId();
        try {
            Integer count = reviewMapper.countByUserId(userId);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("计算评论类成就进度失败: userId={}, achievementId={}", userId, achievement.getId(), e);
            return currentProgress;
        }
    }
}
