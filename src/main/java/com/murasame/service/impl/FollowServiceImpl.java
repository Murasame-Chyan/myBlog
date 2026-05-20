package com.murasame.service.impl;

import com.murasame.entity.Users;
import com.murasame.mapper.FollowMapper;
import com.murasame.mapper.UserMapper;
import com.murasame.service.FollowService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class FollowServiceImpl implements FollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowServiceImpl.class);
    private static final String FOLLOWING_KEY = "user:following:";
    private static final String FOLLOWERS_KEY = "user:followers:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private FollowMapper followMapper;

    @Resource
    private UserMapper userMapper;

    @PostConstruct
    public void warmup() {
        try {
            List<AbstractMap.SimpleEntry<Long, Long>> all = followMapper.findAllForWarmup();
            if (all != null && !all.isEmpty()) {
                for (var entry : all) {
                    Long followerId = entry.getKey();
                    Long followeeId = entry.getValue();
                    stringRedisTemplate.opsForSet().add(FOLLOWING_KEY + followerId, followeeId.toString());
                    stringRedisTemplate.opsForSet().add(FOLLOWERS_KEY + followeeId, followerId.toString());
                }
                log.info("Redis follows warmup completed: {} records", all.size());
            }
        } catch (Exception e) {
            log.warn("Redis follows warmup failed (DB may not be ready): {}", e.getMessage());
        }
    }

    @Override
    public void follow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            return;
        }
        stringRedisTemplate.opsForSet().add(FOLLOWING_KEY + followerId, followeeId.toString());
        stringRedisTemplate.opsForSet().add(FOLLOWERS_KEY + followeeId, followerId.toString());
        persistFollow(followerId, followeeId);
    }

    @Override
    public void unfollow(Long followerId, Long followeeId) {
        stringRedisTemplate.opsForSet().remove(FOLLOWING_KEY + followerId, followeeId.toString());
        stringRedisTemplate.opsForSet().remove(FOLLOWERS_KEY + followeeId, followerId.toString());
        persistUnfollow(followerId, followeeId);
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) return false;
        Boolean exists = stringRedisTemplate.opsForSet().isMember(FOLLOWING_KEY + followerId, followeeId.toString());
        if (Boolean.TRUE.equals(exists)) return true;
        return followMapper.exists(followerId, followeeId) > 0;
    }

    @Override
    public List<Users> getFollowers(Long userId, int page, int pageSize) {
        List<Long> followerIds = getCachedOrDbFollowerIds(userId);
        return paginateAndFetchUsers(followerIds, page, pageSize);
    }

    @Override
    public List<Users> getFollowing(Long userId, int page, int pageSize) {
        List<Long> followeeIds = getCachedOrDbFollowingIds(userId);
        return paginateAndFetchUsers(followeeIds, page, pageSize);
    }

    @Override
    public int countFollowers(Long userId) {
        Long size = stringRedisTemplate.opsForSet().size(FOLLOWERS_KEY + userId);
        if (size != null && size > 0) return size.intValue();
        Users user = userMapper.getUserById(userId);
        return user != null && user.getFollowerCount() != null ? user.getFollowerCount() : 0;
    }

    @Override
    public int countFollowing(Long userId) {
        Long size = stringRedisTemplate.opsForSet().size(FOLLOWING_KEY + userId);
        if (size != null && size > 0) return size.intValue();
        Users user = userMapper.getUserById(userId);
        return user != null && user.getFollowingCount() != null ? user.getFollowingCount() : 0;
    }

    private List<Long> getCachedOrDbFollowingIds(Long userId) {
        Set<String> members = stringRedisTemplate.opsForSet().members(FOLLOWING_KEY + userId);
        if (members != null && !members.isEmpty()) {
            return members.stream().map(Long::parseLong).toList();
        }
        List<Long> dbIds = followMapper.findFollowingIds(userId);
        if (!dbIds.isEmpty()) {
            for (Long id : dbIds) {
                stringRedisTemplate.opsForSet().add(FOLLOWING_KEY + userId, id.toString());
            }
        }
        return dbIds;
    }

    private List<Long> getCachedOrDbFollowerIds(Long userId) {
        Set<String> members = stringRedisTemplate.opsForSet().members(FOLLOWERS_KEY + userId);
        if (members != null && !members.isEmpty()) {
            return members.stream().map(Long::parseLong).toList();
        }
        List<Long> dbIds = followMapper.findFollowerIds(userId);
        if (!dbIds.isEmpty()) {
            for (Long id : dbIds) {
                stringRedisTemplate.opsForSet().add(FOLLOWERS_KEY + userId, id.toString());
            }
        }
        return dbIds;
    }

    private List<Users> paginateAndFetchUsers(List<Long> userIds, int page, int pageSize) {
        if (userIds.isEmpty()) return Collections.emptyList();
        int from = (page - 1) * pageSize;
        if (from >= userIds.size()) return Collections.emptyList();
        int to = Math.min(from + pageSize, userIds.size());
        List<Users> result = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Users user = userMapper.getUserById(userIds.get(i));
            if (user != null) {
                // 脱敏：仅返回公开字段
                user.setPassword(null);
                user.setEmail(null);
            }
            result.add(user);
        }
        return result;
    }

    @Async
    protected void persistFollow(Long followerId, Long followeeId) {
        try {
            int affected = followMapper.insert(followerId, followeeId);
            if (affected > 0) {
                userMapper.incrementFollowingCount(followerId);
                userMapper.incrementFollowerCount(followeeId);
            }
        } catch (Exception e) {
            log.warn("Failed to persist follow: followerId={}, followeeId={}", followerId, followeeId);
        }
    }

    @Async
    protected void persistUnfollow(Long followerId, Long followeeId) {
        try {
            int affected = followMapper.delete(followerId, followeeId);
            if (affected > 0) {
                userMapper.decrementFollowingCount(followerId);
                userMapper.decrementFollowerCount(followeeId);
            }
        } catch (Exception e) {
            log.warn("Failed to persist unfollow: followerId={}, followeeId={}", followerId, followeeId);
        }
    }
}
