package com.steam.service;

import com.steam.dto.PageResult;
import com.steam.entity.Friendship;
import com.steam.entity.User;
import com.steam.mapper.FriendshipMapper;
import com.steam.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;

    public List<Friendship> getFriends(Long userId) {
        return friendshipMapper.findByUserIdAndStatus(userId, "ACCEPTED");
    }

    public List<Friendship> getPendingRequests(Long userId) {
        return friendshipMapper.findPendingRequests(userId);
    }

    public List<Friendship> getSentRequests(Long userId) {
        return friendshipMapper.findSentRequests(userId);
    }

    public Long getPendingRequestCount(Long userId) {
        return friendshipMapper.countPendingRequests(userId);
    }

    public List<Friendship> getBlockedUsers(Long userId) {
        return friendshipMapper.findByUserIdAndStatus(userId, "BLOCKED");
    }

    @Transactional
    public void sendFriendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        User friend = userMapper.findById(friendId);
        if (friend == null) {
            throw new RuntimeException("用户不存在");
        }

        Friendship existing = friendshipMapper.findByUserIdAndFriendId(userId, friendId);
        if (existing != null) {
            if ("BLOCKED".equals(existing.getStatus())) {
                throw new RuntimeException("您已拉黑该用户");
            }
            if ("PENDING".equals(existing.getStatus())) {
                throw new RuntimeException("已发送过好友请求");
            }
            if ("ACCEPTED".equals(existing.getStatus())) {
                throw new RuntimeException("已经是好友了");
            }
        }

        Friendship reverseExisting = friendshipMapper.findByUserIdAndFriendId(friendId, userId);
        if (reverseExisting != null && "PENDING".equals(reverseExisting.getStatus())) {
            acceptFriendRequest(userId, friendId);
            return;
        }

        Friendship friendship = new Friendship();
        friendship.setUserId(userId);
        friendship.setFriendId(friendId);
        friendship.setStatus("PENDING");
        friendship.setActionUserId(userId);
        friendshipMapper.insert(friendship);

        log.info("用户 {} 向用户 {} 发送好友请求", userId, friendId);
    }

    @Transactional
    public void acceptFriendRequest(Long userId, Long friendId) {
        Friendship incoming = friendshipMapper.findByUserIdAndFriendId(friendId, userId);
        if (incoming == null || !"PENDING".equals(incoming.getStatus())) {
            throw new RuntimeException("好友请求不存在");
        }

        friendshipMapper.updateStatus(friendId, userId, "ACCEPTED", userId);

        Friendship reverse = friendshipMapper.findByUserIdAndFriendId(userId, friendId);
        if (reverse == null) {
            Friendship newReverse = new Friendship();
            newReverse.setUserId(userId);
            newReverse.setFriendId(friendId);
            newReverse.setStatus("ACCEPTED");
            newReverse.setActionUserId(userId);
            friendshipMapper.insert(newReverse);
        } else {
            friendshipMapper.updateStatus(userId, friendId, "ACCEPTED", userId);
        }

        log.info("用户 {} 接受了用户 {} 的好友请求", userId, friendId);
    }

    @Transactional
    public void rejectFriendRequest(Long userId, Long friendId) {
        Friendship incoming = friendshipMapper.findByUserIdAndFriendId(friendId, userId);
        if (incoming == null || !"PENDING".equals(incoming.getStatus())) {
            throw new RuntimeException("好友请求不存在");
        }

        friendshipMapper.delete(friendId, userId);

        Friendship reverse = friendshipMapper.findByUserIdAndFriendId(userId, friendId);
        if (reverse != null && "PENDING".equals(reverse.getStatus())) {
            friendshipMapper.delete(userId, friendId);
        }

        log.info("用户 {} 拒绝了用户 {} 的好友请求", userId, friendId);
    }

    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        friendshipMapper.delete(userId, friendId);
        friendshipMapper.delete(friendId, userId);
        log.info("用户 {} 删除了好友 {}", userId, friendId);
    }

    @Transactional
    public void blockUser(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new RuntimeException("不能拉黑自己");
        }

        User friend = userMapper.findById(friendId);
        if (friend == null) {
            throw new RuntimeException("用户不存在");
        }

        Friendship existing = friendshipMapper.findByUserIdAndFriendId(userId, friendId);
        if (existing == null) {
            Friendship friendship = new Friendship();
            friendship.setUserId(userId);
            friendship.setFriendId(friendId);
            friendship.setStatus("BLOCKED");
            friendship.setActionUserId(userId);
            friendshipMapper.insert(friendship);
        } else {
            friendshipMapper.updateStatus(userId, friendId, "BLOCKED", userId);
        }

        friendshipMapper.delete(friendId, userId);

        log.info("用户 {} 拉黑了用户 {}", userId, friendId);
    }

    @Transactional
    public void unblockUser(Long userId, Long friendId) {
        Friendship existing = friendshipMapper.findByUserIdAndFriendId(userId, friendId);
        if (existing == null || !"BLOCKED".equals(existing.getStatus())) {
            throw new RuntimeException("未拉黑该用户");
        }

        friendshipMapper.delete(userId, friendId);
        log.info("用户 {} 取消拉黑用户 {}", userId, friendId);
    }

    public PageResult<User> searchUsers(Long userId, String keyword, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<User> users = userMapper.searchUsers(keyword, offset, size);
        Long total = userMapper.countUsersByKeyword(keyword);

        List<User> result = new ArrayList<>();
        for (User user : users) {
            if (user.getId().equals(userId)) {
                continue;
            }
            user.setPassword(null);
            user.setEmail(null);
            user.setBalance(null);
            result.add(user);
        }

        return PageResult.of(result, total, page, size);
    }

    public Friendship getFriendshipStatus(Long userId, Long targetUserId) {
        return friendshipMapper.findByUserIdAndFriendId(userId, targetUserId);
    }
}
