package com.murasame.service;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    // Redis 计数实现登录失败锁定：5 次失败后锁定 5 分钟
    private static final String PREFIX = "login:attempts:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String checkLocked(String email) {
        String key = PREFIX + email;
        String countStr = stringRedisTemplate.opsForValue().get(key);
        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            if (count >= MAX_ATTEMPTS) {
                Long remainingSeconds = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
                // 用 ceil 向上取整，确保用户在剩余不足 1 分钟时看到的是 "1 分钟" 而不是 "0 分钟"
                long remainingMinutes = remainingSeconds != null && remainingSeconds > 0
                        ? (long) Math.ceil(remainingSeconds / 60.0)
                        : 5;
                return "账号已被锁定，请 " + remainingMinutes + " 分钟后再试";
            }
        }
        return null;
    }

    // 仅在首次失败时设置 TTL，后续失败累加计数但不刷新过期时间
    // 这样 5 分钟内连续失败 5 次后被锁定，锁定时长从第一次失败开始算
    public void recordFailedAttempt(String email) {
        String key = PREFIX + email;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, LOCK_DURATION);
        }
    }

    public void resetAttempts(String email) {
        stringRedisTemplate.delete(PREFIX + email);
    }
}
