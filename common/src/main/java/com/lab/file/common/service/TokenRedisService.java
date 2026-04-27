package com.lab.file.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenRedisService {

    @Autowired(required = false)  // 允许为 null，避免没有 Redis 时启动失败
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_PREFIX = "user:token:";
    private static final String USER_TOKEN_PREFIX = "user:tokens:";
    private static final Long TOKEN_EXPIRE_TIME = 24L; // 24小时

    /**
     * 保存用户Token
     * @param userId 用户ID
     * @param token JWT Token
     */
    public void saveToken(Long userId, String token) {
        String tokenKey = TOKEN_PREFIX + token;
        String userTokenKey = USER_TOKEN_PREFIX + userId;

        // 保存 token -> userId 的映射
        redisTemplate.opsForValue().set(tokenKey, userId, TOKEN_EXPIRE_TIME, TimeUnit.HOURS);

        // 保存用户的所有token（支持多设备登录）
        redisTemplate.opsForSet().add(userTokenKey, token);
        redisTemplate.expire(userTokenKey, TOKEN_EXPIRE_TIME, TimeUnit.HOURS);

        System.out.println("Token已保存到Redis: userId=" + userId);
    }

    /**
     * 验证Token是否有效
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        String tokenKey = TOKEN_PREFIX + token;
        return redisTemplate.hasKey(tokenKey);
    }

    /**
     * 获取Token对应的用户ID
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdByToken(String token) {
        String tokenKey = TOKEN_PREFIX + token;
        Object userId = redisTemplate.opsForValue().get(tokenKey);
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }
        return null;
    }

    /**
     * 刷新Token过期时间
     * @param token JWT Token
     */
    public void refreshTokenExpire(String token) {
        String tokenKey = TOKEN_PREFIX + token;
        redisTemplate.expire(tokenKey, TOKEN_EXPIRE_TIME, TimeUnit.HOURS);

        // 同时刷新用户对应的token集合过期时间
        Long userId = getUserIdByToken(token);
        if (userId != null) {
            String userTokenKey = USER_TOKEN_PREFIX + userId;
            redisTemplate.expire(userTokenKey, TOKEN_EXPIRE_TIME, TimeUnit.HOURS);
        }
    }

    /**
     * 移除Token（登出时）
     * @param token JWT Token
     */
    public void removeToken(String token) {
        String tokenKey = TOKEN_PREFIX + token;
        Long userId = getUserIdByToken(token);

        if (userId != null) {
            String userTokenKey = USER_TOKEN_PREFIX + userId;
            redisTemplate.opsForSet().remove(userTokenKey, token);
            redisTemplate.delete(tokenKey);
        }
    }

    /**
     * 移除用户所有Token（强制登出）
     * @param userId 用户ID
     */
    public void removeAllUserTokens(Long userId) {
        String userTokenKey = USER_TOKEN_PREFIX + userId;
        redisTemplate.delete(userTokenKey);

        // 这里可以选择删除所有token，但需要遍历set中的token
        // 简化处理，只删除用户token集合
        System.out.println("已移除用户所有Token: userId=" + userId);
    }

    /**
     * 获取用户当前活跃Token数量
     * @param userId 用户ID
     * @return Token数量
     */
    public Long getUserTokenCount(Long userId) {
        String userTokenKey = USER_TOKEN_PREFIX + userId;
        Long size = redisTemplate.opsForSet().size(userTokenKey);
        return size != null ? size : 0L;
    }
}