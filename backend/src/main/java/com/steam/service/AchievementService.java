package com.steam.service;

import com.steam.achievement.AchievementEngine;
import com.steam.achievement.AchievementEvent;
import com.steam.dto.UnlockedAchievementVO;
import com.steam.entity.Achievement;
import com.steam.entity.UserAchievement;
import com.steam.mapper.AchievementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementMapper achievementMapper;
    private final AchievementEngine achievementEngine;
    private final ActivityService activityService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<UserAchievement> getUserAchievements(Long userId) {
        List<Achievement> allAchievements = achievementMapper.findAllActive();
        List<UserAchievement> userAchievements = achievementMapper.findUserAchievements(userId);

        Map<Long, UserAchievement> userAchMap = userAchievements.stream()
                .collect(Collectors.toMap(ua -> ua.getAchievementId(), ua -> ua));

        List<UserAchievement> result = new ArrayList<>();
        for (Achievement ach : allAchievements) {
            UserAchievement ua = userAchMap.get(ach.getId());
            if (ua == null) {
                ua = new UserAchievement();
                ua.setUserId(userId);
                ua.setAchievementId(ach.getId());
                ua.setProgress(0);
                ua.setTargetValue(ach.getTargetValue());
                ua.setIsUnlocked(0);
                ua.setAchievement(ach);
            } else if (ua.getAchievement() == null) {
                ua.setAchievement(ach);
            }
            result.add(ua);
        }

        result.sort((a, b) -> {
            int unlockedCompare = Integer.compare(
                    b.getIsUnlocked() != null ? b.getIsUnlocked() : 0,
                    a.getIsUnlocked() != null ? a.getIsUnlocked() : 0);
            if (unlockedCompare != 0) return unlockedCompare;
            int rarityCompare = Integer.compare(
                    b.getAchievement().getRarity() != null ? b.getAchievement().getRarity() : 0,
                    a.getAchievement().getRarity() != null ? a.getAchievement().getRarity() : 0);
            if (rarityCompare != 0) return rarityCompare;
            return Integer.compare(
                    a.getAchievement().getSortOrder() != null ? a.getAchievement().getSortOrder() : 0,
                    b.getAchievement().getSortOrder() != null ? b.getAchievement().getSortOrder() : 0);
        });

        return result;
    }

    public List<UserAchievement> getRecentUnlocked(Long userId, Integer limit) {
        return achievementMapper.findRecentUnlocked(userId, limit != null ? limit : 5);
    }

    public Map<String, Object> getAchievementStats(Long userId) {
        List<UserAchievement> all = getUserAchievements(userId);
        long unlockedCount = all.stream().filter(ua -> ua.getIsUnlocked() != null && ua.getIsUnlocked() == 1).count();
        int totalPoints = all.stream()
                .filter(ua -> ua.getIsUnlocked() != null && ua.getIsUnlocked() == 1)
                .mapToInt(ua -> ua.getAchievement().getPoints() != null ? ua.getAchievement().getPoints() : 0)
                .sum();
        int totalCount = all.size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("unlockedCount", unlockedCount);
        stats.put("totalPoints", totalPoints);
        stats.put("completionRate", totalCount > 0 ? (double) unlockedCount / totalCount * 100 : 0);
        return stats;
    }

    public List<UserAchievement> triggerOrderPaid(Long userId, Long orderId) {
        AchievementEvent event = new AchievementEvent(AchievementEvent.ORDER_PAID, userId)
                .put("orderId", orderId);
        List<UserAchievement> unlocked = achievementEngine.processEvent(event);
        AchievementEvent libEvent = new AchievementEvent(AchievementEvent.LIBRARY_UPDATED, userId)
                .put("orderId", orderId);
        unlocked.addAll(achievementEngine.processEvent(libEvent));

        try {
            activityService.createAchievementActivity(userId, unlocked);
        } catch (Exception e) {
            log.error("创建成就动态失败: userId={}", userId, e);
        }

        return unlocked;
    }

    public List<UserAchievement> triggerReviewCreated(Long userId, Long reviewId) {
        AchievementEvent event = new AchievementEvent(AchievementEvent.REVIEW_CREATED, userId)
                .put("reviewId", reviewId);
        List<UserAchievement> unlocked = achievementEngine.processEvent(event);

        try {
            activityService.createAchievementActivity(userId, unlocked);
        } catch (Exception e) {
            log.error("创建成就动态失败: userId={}", userId, e);
        }

        return unlocked;
    }

    public List<UserAchievement> triggerPlaytimeUpdated(Long userId, Long gameId, Integer playTime) {
        AchievementEvent event = new AchievementEvent(AchievementEvent.PLAYTIME_UPDATED, userId)
                .put("gameId", gameId)
                .put("playTime", playTime);
        List<UserAchievement> unlocked = achievementEngine.processEvent(event);

        try {
            activityService.createAchievementActivity(userId, unlocked);
        } catch (Exception e) {
            log.error("创建成就动态失败: userId={}", userId, e);
        }

        return unlocked;
    }

    public List<UserAchievement> triggerLibraryUpdated(Long userId, Long gameId) {
        AchievementEvent event = new AchievementEvent(AchievementEvent.LIBRARY_UPDATED, userId)
                .put("gameId", gameId);
        List<UserAchievement> unlocked = achievementEngine.processEvent(event);

        try {
            activityService.createAchievementActivity(userId, unlocked);
        } catch (Exception e) {
            log.error("创建成就动态失败: userId={}", userId, e);
        }

        return unlocked;
    }

    public List<UserAchievement> triggerLibraryUpdated(Long userId) {
        return triggerLibraryUpdated(userId, null);
    }

    public List<UnlockedAchievementVO> toVOList(List<UserAchievement> unlocked) {
        if (unlocked == null || unlocked.isEmpty()) return Collections.emptyList();
        List<UnlockedAchievementVO> voList = new ArrayList<>();
        for (UserAchievement ua : unlocked) {
            Achievement ach = ua.getAchievement();
            if (ach == null) continue;
            UnlockedAchievementVO vo = new UnlockedAchievementVO();
            vo.setAchievementId(ach.getId());
            vo.setCode(ach.getCode());
            vo.setName(ach.getName());
            vo.setDescription(ach.getDescription());
            vo.setIcon(ach.getIcon());
            vo.setCategory(ach.getCategory());
            vo.setRarity(ach.getRarity());
            vo.setPoints(ach.getPoints());
            vo.setUnlockedAt(ua.getUnlockedAt() != null ? ua.getUnlockedAt().format(FMT) : null);
            voList.add(vo);
        }
        return voList;
    }
}
