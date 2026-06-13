package com.steam.service;

import com.steam.entity.Game;
import com.steam.entity.UserLibrary;
import com.steam.entity.UserPreorder;
import com.steam.mapper.GameMapper;
import com.steam.mapper.UserLibraryMapper;
import com.steam.mapper.UserPreorderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreorderService {

    private final UserPreorderMapper userPreorderMapper;
    private final UserLibraryMapper userLibraryMapper;
    private final GameMapper gameMapper;
    private final AchievementService achievementService;

    public List<UserPreorder> getUserPreorders(Long userId) {
        return userPreorderMapper.findByUserId(userId);
    }

    public int getPreorderCount(Long userId) {
        return userPreorderMapper.countPendingByUserId(userId);
    }

    public boolean hasPreordered(Long userId, Long gameId) {
        return userPreorderMapper.existsPendingByUserIdAndGameId(userId, gameId);
    }

    @Transactional
    public int processDuePreorders() {
        LocalDateTime now = LocalDateTime.now();
        List<UserPreorder> dueList = userPreorderMapper.findReadyToConvert(now);
        if (dueList == null || dueList.isEmpty()) {
            return 0;
        }
        int converted = 0;
        for (UserPreorder pre : dueList) {
            try {
                convertPreorderToLibrary(pre);
                converted++;
            } catch (Exception e) {
                log.error("转正预购失败, preorderId={}, userId={}, gameId={}",
                        pre.getId(), pre.getUserId(), pre.getGameId(), e);
            }
        }
        if (converted > 0) {
            log.info("预购转正处理完成, 共转正 {} 个", converted);
        }
        return converted;
    }

    @Transactional
    public int processDuePreordersForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<UserPreorder> all = userPreorderMapper.findByUserId(userId);
        if (all == null || all.isEmpty()) return 0;
        int converted = 0;
        for (UserPreorder pre : all) {
            if (!"PENDING_RELEASE".equals(pre.getStatus())) continue;
            Game game = pre.getGame();
            if (game == null) game = gameMapper.findRawById(pre.getGameId());
            if (game == null) continue;
            String rs = game.getReleaseStatus() == null ? "RELEASED" : game.getReleaseStatus();
            boolean due = "RELEASED".equals(rs)
                    || (game.getPreorderUnlockDate() != null && !game.getPreorderUnlockDate().isAfter(now))
                    || ("CROWDFUNDING".equals(rs) && game.getCrowdfundingGoal() != null
                        && game.getCurrentFunding() != null
                        && game.getCurrentFunding().compareTo(game.getCrowdfundingGoal()) >= 0);
            if (due) {
                try {
                    convertPreorderToLibrary(pre);
                    converted++;
                } catch (Exception e) {
                    log.error("用户登录转正预购失败, preorderId={}", pre.getId(), e);
                }
            }
        }
        return converted;
    }

    private void convertPreorderToLibrary(UserPreorder pre) {
        if (!userLibraryMapper.existsByUserIdAndGameId(pre.getUserId(), pre.getGameId())) {
            UserLibrary lib = new UserLibrary();
            lib.setUserId(pre.getUserId());
            lib.setGameId(pre.getGameId());
            lib.setOrderId(pre.getOrderId());
            userLibraryMapper.insert(lib);
        }
        userPreorderMapper.markAsReleased(pre.getId(), LocalDateTime.now());
        Game game = gameMapper.findRawById(pre.getGameId());
        if (game != null && !"RELEASED".equals(game.getReleaseStatus())) {
            gameMapper.updateReleaseStatus(game.getId(), "RELEASED");
        }
        try {
            achievementService.triggerLibraryUpdated(pre.getUserId(), pre.getGameId());
        } catch (Exception e) {
            log.error("触发成就失败: userId={}, gameId={}", pre.getUserId(), pre.getGameId(), e);
        }
    }
}
