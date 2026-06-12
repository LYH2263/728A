package com.steam.achievement;

import com.steam.entity.Achievement;

public interface AchievementRule {
    boolean matches(Achievement achievement, AchievementEvent event);
    int calculateProgress(Achievement achievement, AchievementEvent event, int currentProgress);
}
