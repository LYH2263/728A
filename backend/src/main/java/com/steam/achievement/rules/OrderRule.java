package com.steam.achievement.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steam.achievement.AchievementEvent;
import com.steam.achievement.AchievementRule;
import com.steam.entity.Achievement;
import com.steam.entity.User;
import com.steam.mapper.OrderMapper;
import com.steam.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component("ORDER_PAID")
@RequiredArgsConstructor
public class OrderRule implements AchievementRule {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean matches(Achievement achievement, AchievementEvent event) {
        return AchievementEvent.ORDER_PAID.equals(event.getEventType());
    }

    @Override
    public int calculateProgress(Achievement achievement, AchievementEvent event, int currentProgress) {
        Long userId = event.getUserId();
        String code = achievement.getCode();

        try {
            if ("FIRST_PURCHASE".equals(code)) {
                List<?> orders = orderMapper.findCompletedByUserId(userId);
                return (orders != null && orders.size() >= 1) ? 1 : 0;
            }

            if ("SHOPAHOLIC".equals(code)) {
                Integer count = orderMapper.countPaidGamesByUserId(userId);
                return count != null ? count : 0;
            }

            if ("SPEED_BUYER".equals(code)) {
                if (currentProgress >= 1) return 1;
                String ruleConfig = achievement.getRuleConfig();
                if (ruleConfig != null && !ruleConfig.isEmpty()) {
                    JsonNode config = objectMapper.readTree(ruleConfig);
                    int withinHours = config.has("within_hours") ? config.get("within_hours").asInt(24) : 24;
                    User user = userMapper.findById(userId);
                    if (user != null && user.getCreatedAt() != null) {
                        LocalDateTime createdAt = user.getCreatedAt();
                        LocalDateTime now = LocalDateTime.now();
                        long hoursBetween = Duration.between(createdAt, now).toHours();
                        if (hoursBetween <= withinHours) {
                            return 1;
                        }
                    }
                }
                return 0;
            }

            return Math.min(currentProgress + 1, achievement.getTargetValue());
        } catch (Exception e) {
            log.error("计算购买类成就进度失败: userId={}, achievementId={}", userId, achievement.getId(), e);
            return currentProgress;
        }
    }
}
