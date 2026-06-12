package com.steam.service;

import com.steam.dto.PageResult;
import com.steam.entity.Activity;
import com.steam.entity.Achievement;
import com.steam.entity.Game;
import com.steam.entity.GameReview;
import com.steam.entity.Order;
import com.steam.entity.OrderItem;
import com.steam.entity.UserAchievement;
import com.steam.mapper.ActivityMapper;
import com.steam.mapper.AchievementMapper;
import com.steam.mapper.GameMapper;
import com.steam.mapper.GameReviewMapper;
import com.steam.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final GameMapper gameMapper;
    private final AchievementMapper achievementMapper;
    private final GameReviewMapper gameReviewMapper;
    private final OrderMapper orderMapper;

    public PageResult<Activity> getFriendActivities(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Activity> activities = activityMapper.findFriendActivities(userId, offset, size);
        Long total = activityMapper.countFriendActivities(userId);
        return PageResult.of(activities, total, page, size);
    }

    public PageResult<Activity> getUserActivities(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Activity> activities = activityMapper.findUserActivities(userId, offset, size);
        Long total = activityMapper.countUserActivities(userId);
        return PageResult.of(activities, total, page, size);
    }

    @Transactional
    public void createPurchaseActivity(Long userId, Long orderId) {
        try {
            Order order = orderMapper.findById(orderId);
            if (order == null) {
                log.warn("订单不存在，无法创建购买动态: orderId={}", orderId);
                return;
            }

            List<OrderItem> orderItems = orderMapper.findOrderItemsByOrderId(order.getId());
            for (OrderItem item : orderItems) {
                Activity activity = new Activity();
                activity.setUserId(userId);
                activity.setType("PURCHASE");
                activity.setGameId(item.getGameId());
                activity.setGameTitle(item.getGameTitle());
                activity.setGameCover(item.getGameCover());
                activityMapper.insert(activity);
                log.debug("创建购买动态: userId={}, gameId={}", userId, item.getGameId());
            }
        } catch (Exception e) {
            log.error("创建购买动态失败: userId={}, orderId={}", userId, orderId, e);
        }
    }

    @Transactional
    public void createAchievementActivity(Long userId, List<UserAchievement> achievements) {
        if (achievements == null || achievements.isEmpty()) {
            return;
        }

        try {
            for (UserAchievement ua : achievements) {
                if (ua.getIsUnlocked() == null || ua.getIsUnlocked() != 1) {
                    continue;
                }

                Achievement ach = achievementMapper.findById(ua.getAchievementId());
                if (ach == null) {
                    continue;
                }

                Activity activity = new Activity();
                activity.setUserId(userId);
                activity.setType("ACHIEVEMENT");
                activity.setAchievementId(ach.getId());
                activity.setAchievementName(ach.getName());
                activityMapper.insert(activity);
                log.debug("创建成就动态: userId={}, achievementId={}", userId, ach.getId());
            }
        } catch (Exception e) {
            log.error("创建成就动态失败: userId={}", userId, e);
        }
    }

    @Transactional
    public void createReviewActivity(Long userId, Long reviewId) {
        try {
            GameReview review = gameReviewMapper.findById(reviewId);
            if (review == null) {
                log.warn("评论不存在，无法创建评论动态: reviewId={}", reviewId);
                return;
            }

            Game game = gameMapper.findById(review.getGameId());

            Activity activity = new Activity();
            activity.setUserId(userId);
            activity.setType("REVIEW");
            activity.setGameId(review.getGameId());
            activity.setGameTitle(game != null ? game.getTitle() : null);
            activity.setGameCover(game != null ? game.getCoverImage() : null);
            activity.setReviewId(reviewId);
            activity.setReviewRating(review.getRating());
            activity.setReviewContent(review.getContent());
            activityMapper.insert(activity);

            log.debug("创建评论动态: userId={}, reviewId={}", userId, reviewId);
        } catch (Exception e) {
            log.error("创建评论动态失败: userId={}, reviewId={}", userId, reviewId, e);
        }
    }
}
