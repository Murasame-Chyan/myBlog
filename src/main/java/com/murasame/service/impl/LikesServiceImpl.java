package com.murasame.service.impl;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.mapper.BlogMapper;
import com.murasame.mapper.UserLikeMapper;
import com.murasame.service.LikesService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LikesServiceImpl implements LikesService {

    private static final Logger log = LoggerFactory.getLogger(LikesServiceImpl.class);
    private static final String KEY_PREFIX = "user:likes:";

    @Resource
    private StringRedisTemplate redisTemplate;

    @Resource
    private UserLikeMapper userLikeMapper;

    @Resource
    private BlogMapper blogMapper;

    @PostConstruct
    public void warmup() {
        try {
            List<java.util.AbstractMap.SimpleEntry<Long, Long>> all = userLikeMapper.findAllForWarmup();
            if (all != null && !all.isEmpty()) {
                for (var entry : all) {
                    String key = KEY_PREFIX + entry.getKey();
                    redisTemplate.opsForSet().add(key, entry.getValue().toString());
                }
                log.info("Redis likes warmup completed: {} records", all.size());
            }
        } catch (Exception e) {
            log.warn("Redis likes warmup failed (DB may not be ready): {}", e.getMessage());
        }
    }

    @Override
    public void like(Long userId, Long blogId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.opsForSet().add(key, blogId.toString());
        persistLike(userId, blogId);
    }

    @Override
    public void unlike(Long userId, Long blogId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.opsForSet().remove(key, blogId.toString());
        persistUnlike(userId, blogId);
    }

    @Override
    public boolean isLiked(Long userId, Long blogId) {
        if (userId == null) return false;
        String key = KEY_PREFIX + userId;
        Boolean exists = redisTemplate.opsForSet().isMember(key, blogId.toString());
        if (Boolean.TRUE.equals(exists)) return true;
        // fallback to DB
        return userLikeMapper.exists(userId, blogId) > 0;
    }

    @Override
    public List<BlogBriefVO> getLikedBlogs(Long userId, int limit) {
        if (userId == null) return Collections.emptyList();
        String key = KEY_PREFIX + userId;
        Set<String> members = redisTemplate.opsForSet().members(key);
        if (members == null || members.isEmpty()) {
            // fallback to DB
            List<Long> dbIds = userLikeMapper.findBlogIdsByUserId(userId);
            if (dbIds.isEmpty()) return Collections.emptyList();
            // restore Redis
            for (Long id : dbIds) {
                redisTemplate.opsForSet().add(key, id.toString());
            }
            return fetchBlogsByIds(dbIds, limit);
        }
        List<Long> ids = members.stream()
                .map(Long::parseLong)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(ids); // most recent first (Redis Set doesn't guarantee order, fallback to DB for proper ordering)
        // Use DB ordering since Redis Set is unordered
        List<Long> dbOrdered = userLikeMapper.findBlogIdsByUserId(userId);
        if (!dbOrdered.isEmpty()) {
            return fetchBlogsByIds(dbOrdered, limit);
        }
        return fetchBlogsByIds(ids, limit);
    }

    private List<BlogBriefVO> fetchBlogsByIds(List<Long> ids, int limit) {
        List<Long> limited = ids.size() > limit ? ids.subList(0, limit) : ids;
        if (limited.isEmpty()) return Collections.emptyList();
        return blogMapper.getBlogsByIds(limited);
    }

    @Async
    protected void persistLike(Long userId, Long blogId) {
        try {
            userLikeMapper.insert(userId, blogId);
        } catch (Exception e) {
            log.warn("Failed to persist like to MySQL: userId={}, blogId={}", userId, blogId);
        }
    }

    @Async
    protected void persistUnlike(Long userId, Long blogId) {
        try {
            userLikeMapper.delete(userId, blogId);
        } catch (Exception e) {
            log.warn("Failed to persist unlike to MySQL: userId={}, blogId={}", userId, blogId);
        }
    }
}
