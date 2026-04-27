package com.lab.file.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SlidingWindowRateLimiter {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "rate_limit:";

    /**
     * 滑动窗口限流
     * @param key 限流key（IP或用户ID）
     * @param windowSeconds 时间窗口（秒）
     * @param limit 限制次数
     * @return 是否允许通过
     */
    public boolean isAllowed(String key, int windowSeconds, int limit) {
        String redisKey = KEY_PREFIX + key;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        // 1. 移除窗口外的数据
        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

        // 2. 获取当前窗口内的请求数
        Long count = redisTemplate.opsForZSet().zCard(redisKey);

        // 3. 判断是否超过限制
        if (count == null || count < limit) {
            // 允许访问，添加当前请求
            redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
            // 设置过期时间（比窗口多10秒）
            redisTemplate.expire(redisKey, windowSeconds + 10, TimeUnit.SECONDS);
            return true;
        }

        // 超过限制，拒绝访问
        return false;
    }
}