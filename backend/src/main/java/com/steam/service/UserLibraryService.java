package com.steam.service;

import com.steam.entity.UserLibrary;
import com.steam.mapper.UserLibraryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLibraryService {
    
    private final UserLibraryMapper userLibraryMapper;
    private final AchievementService achievementService;
    
    public List<UserLibrary> getUserLibrary(Long userId) {
        return userLibraryMapper.findByUserId(userId);
    }
    
    public boolean ownsGame(Long userId, Long gameId) {
        return userLibraryMapper.existsByUserIdAndGameId(userId, gameId);
    }
    
    public int getLibraryCount(Long userId) {
        return userLibraryMapper.countByUserId(userId);
    }

    @Transactional
    public void updatePlayTime(Long userId, Long gameId, Integer additionalMinutes) {
        if (additionalMinutes == null || additionalMinutes <= 0) {
            return;
        }
        UserLibrary library = userLibraryMapper.findByUserIdAndGameId(userId, gameId);
        if (library == null) {
            throw new RuntimeException("用户未拥有该游戏");
        }
        int newPlayTime = (library.getPlayTime() != null ? library.getPlayTime() : 0) + additionalMinutes;
        LocalDateTime now = LocalDateTime.now();
        userLibraryMapper.updatePlayTime(userId, gameId, newPlayTime, now);
        log.info("更新游玩时长: userId={}, gameId={}, playTime={}分钟", userId, gameId, newPlayTime);

        try {
            achievementService.triggerPlaytimeUpdated(userId, gameId, newPlayTime);
        } catch (Exception e) {
            log.error("触发成就计算失败: userId={}, gameId={}", userId, gameId, e);
        }
    }
}
