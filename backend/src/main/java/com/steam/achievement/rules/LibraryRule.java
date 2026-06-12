package com.steam.achievement.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steam.achievement.AchievementEvent;
import com.steam.achievement.AchievementRule;
import com.steam.entity.Achievement;
import com.steam.mapper.UserLibraryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("LIBRARY_UPDATED")
@RequiredArgsConstructor
public class LibraryRule implements AchievementRule {

    private final UserLibraryMapper userLibraryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean matches(Achievement achievement, AchievementEvent event) {
        return AchievementEvent.LIBRARY_UPDATED.equals(event.getEventType());
    }

    @Override
    public int calculateProgress(Achievement achievement, AchievementEvent event, int currentProgress) {
        Long userId = event.getUserId();
        try {
            String ruleConfig = achievement.getRuleConfig();
            if (ruleConfig != null && !ruleConfig.isEmpty()) {
                JsonNode config = objectMapper.readTree(ruleConfig);
                if (config.has("category_id")) {
                    Long categoryId = config.get("category_id").asLong();
                    Integer count = userLibraryMapper.countByUserIdAndCategory(userId, categoryId);
                    return count != null ? count : 0;
                }
            }
            return userLibraryMapper.countByUserId(userId);
        } catch (Exception e) {
            log.error("计算收藏类成就进度失败: userId={}, achievementId={}", userId, achievement.getId(), e);
            return currentProgress;
        }
    }
}
