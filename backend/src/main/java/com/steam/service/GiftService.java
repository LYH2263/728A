package com.steam.service;

import com.steam.entity.*;
import com.steam.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GiftService {

    private final GiftMapper giftMapper;
    private final UserMapper userMapper;
    private final UserLibraryMapper userLibraryMapper;
    private final UserPreorderMapper userPreorderMapper;
    private final GameMapper gameMapper;
    private final WalletService walletService;
    private final AchievementService achievementService;
    private final ActivityService activityService;

    @Transactional
    public Gift createGift(Long senderId, Long recipientId, OrderItem orderItem, Long orderId, String message) {
        if (senderId.equals(recipientId)) {
            throw new RuntimeException("不能赠送给自己");
        }

        User recipient = userMapper.findById(recipientId);
        if (recipient == null) {
            throw new RuntimeException("收礼用户不存在");
        }

        if (userLibraryMapper.existsByUserIdAndGameId(recipientId, orderItem.getGameId())) {
            throw new RuntimeException("收礼用户已拥有该游戏: " + orderItem.getGameTitle());
        }

        if (userPreorderMapper.existsPendingByUserIdAndGameId(recipientId, orderItem.getGameId())) {
            throw new RuntimeException("收礼用户已预购该游戏: " + orderItem.getGameTitle());
        }

        if (giftMapper.existsPendingByRecipientAndGame(recipientId, orderItem.getGameId())) {
            throw new RuntimeException("收礼用户已有待领取的同款游戏礼物");
        }

        Gift gift = new Gift();
        gift.setGiftNo(generateGiftNo());
        gift.setSenderId(senderId);
        gift.setRecipientId(recipientId);
        gift.setGameId(orderItem.getGameId());
        gift.setGameTitle(orderItem.getGameTitle());
        gift.setGameCover(orderItem.getGameCover());
        gift.setOrderId(orderId);
        gift.setOrderItemId(orderItem.getId());
        gift.setPricePaid(orderItem.getPrice());
        gift.setStatus("PENDING");
        gift.setMessage(message);

        giftMapper.insert(gift);

        log.info("礼物创建成功: giftNo={}, sender={}, recipient={}, game={}",
                gift.getGiftNo(), senderId, recipientId, orderItem.getGameTitle());

        return gift;
    }

    @Transactional
    public Gift claimGift(Long userId, Long giftId) {
        Gift gift = giftMapper.findById(giftId);
        if (gift == null) {
            throw new RuntimeException("礼物不存在");
        }
        if (!gift.getRecipientId().equals(userId)) {
            throw new RuntimeException("无权领取此礼物");
        }
        if (!"PENDING".equals(gift.getStatus())) {
            throw new RuntimeException("礼物状态不正确");
        }

        if (userLibraryMapper.existsByUserIdAndGameId(userId, gift.getGameId())) {
            throw new RuntimeException("您已拥有该游戏，无法领取");
        }
        if (userPreorderMapper.existsPendingByUserIdAndGameId(userId, gift.getGameId())) {
            throw new RuntimeException("您已预购该游戏，无法领取");
        }

        Game game = gameMapper.findRawById(gift.getGameId());
        String releaseStatus = game.getReleaseStatus() == null ? "RELEASED" : game.getReleaseStatus();

        if ("RELEASED".equals(releaseStatus)) {
            UserLibrary library = new UserLibrary();
            library.setUserId(userId);
            library.setGameId(gift.getGameId());
            library.setOrderId(gift.getOrderId());
            userLibraryMapper.insert(library);
        } else {
            UserPreorder preorder = new UserPreorder();
            preorder.setUserId(userId);
            preorder.setGameId(gift.getGameId());
            preorder.setOrderId(gift.getOrderId());
            preorder.setOrderItemId(gift.getOrderItemId());
            preorder.setPricePaid(gift.getPricePaid());
            preorder.setReleaseStatus(releaseStatus);
            userPreorderMapper.insert(preorder);
        }

        LocalDateTime now = LocalDateTime.now();
        giftMapper.updateToClaimed(giftId, now);
        gift.setStatus("CLAIMED");
        gift.setClaimedAt(now);

        log.info("礼物领取成功: giftId={}, recipient={}, game={}", giftId, userId, gift.getGameTitle());

        try {
            achievementService.triggerLibraryUpdated(userId);
        } catch (Exception e) {
            log.error("触发成就计算失败: giftId={}, userId={}", giftId, userId, e);
        }

        try {
            activityService.createGiftClaimActivity(userId, gift);
        } catch (Exception e) {
            log.error("创建礼物领取动态失败: giftId={}, userId={}", giftId, userId, e);
        }

        return gift;
    }

    @Transactional
    public Gift rejectGift(Long userId, Long giftId) {
        Gift gift = giftMapper.findById(giftId);
        if (gift == null) {
            throw new RuntimeException("礼物不存在");
        }
        if (!gift.getRecipientId().equals(userId)) {
            throw new RuntimeException("无权拒绝此礼物");
        }
        if (!"PENDING".equals(gift.getStatus())) {
            throw new RuntimeException("礼物状态不正确");
        }

        LocalDateTime now = LocalDateTime.now();
        giftMapper.updateToRejected(giftId, now);
        gift.setStatus("REJECTED");
        gift.setRejectedAt(now);

        BigDecimal refundAmount = gift.getPricePaid();
        String orderNo = "REFUND_GIFT_" + gift.getGiftNo();
        String description = "礼物被拒收退款: " + gift.getGameTitle() + "，收礼人: " + gift.getRecipientId();
        walletService.refund(gift.getSenderId(), refundAmount, orderNo, description);

        log.info("礼物已拒绝并退款: giftId={}, sender={}, recipient={}, game={}, refund={}",
                giftId, gift.getSenderId(), userId, gift.getGameTitle(), refundAmount);

        return gift;
    }

    public List<Gift> getReceivedGifts(Long userId, String status) {
        List<Gift> gifts;
        if (status != null && !status.isEmpty()) {
            gifts = giftMapper.findByRecipientIdAndStatus(userId, status);
        } else {
            gifts = giftMapper.findByRecipientId(userId);
        }
        enrichGifts(gifts);
        return gifts;
    }

    public List<Gift> getSentGifts(Long userId, String status) {
        List<Gift> gifts;
        if (status != null && !status.isEmpty()) {
            gifts = giftMapper.findBySenderIdAndStatus(userId, status);
        } else {
            gifts = giftMapper.findBySenderId(userId);
        }
        enrichGifts(gifts);
        return gifts;
    }

    public int countPendingGifts(Long userId) {
        return giftMapper.countPendingByRecipientId(userId);
    }

    public Gift getGiftDetail(Long userId, Long giftId) {
        Gift gift = giftMapper.findById(giftId);
        if (gift == null) {
            throw new RuntimeException("礼物不存在");
        }
        if (!gift.getSenderId().equals(userId) && !gift.getRecipientId().equals(userId)) {
            throw new RuntimeException("无权查看此礼物");
        }
        enrichGift(gift);
        return gift;
    }

    private void enrichGifts(List<Gift> gifts) {
        for (Gift gift : gifts) {
            enrichGift(gift);
        }
    }

    private void enrichGift(Gift gift) {
        User sender = userMapper.findById(gift.getSenderId());
        if (sender != null) {
            sender.setPassword(null);
            gift.setSender(sender);
        }
        User recipient = userMapper.findById(gift.getRecipientId());
        if (recipient != null) {
            recipient.setPassword(null);
            gift.setRecipient(recipient);
        }
        Game game = gameMapper.findById(gift.getGameId());
        if (game != null) {
            gift.setGame(game);
        }
    }

    private String generateGiftNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "GIFT" + timestamp + uuid;
    }
}
