package com.gm.imbootstrap.service;

import com.gm.imbootstrap.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author: xexgm
 * @date: 2025/10/1
 * desc: Token管理服务，负责Token的生成、存储、验证和清理
 */
@Slf4j
@Service
public class TokenService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_PREFIX = "user:token:";
    private static final String USER_PREFIX = "token:user:";
    private static final long TOKEN_EXPIRE_TIME = 7; // 7天

    /**
     * 生成并存储Token
     * @param userId 用户ID
     * @param username 用户名
     * @return Token
     */
    public String generateAndStoreToken(Long userId, String username) {
        try {
            // 生成JWT Token
            String token = jwtUtil.generateToken(userId, username);
            
            // 存储到Redis
            storeTokenToRedis(userId, token);
            
            log.info("为用户 {} 生成Token成功", userId);
            return token;
        } catch (Exception e) {
            log.error("为用户 {} 生成Token失败: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Token生成失败", e);
        }
    }

    /**
     * 将Token存储到Redis
     * @param userId 用户ID
     * @param token Token
     */
    private void storeTokenToRedis(Long userId, String token) {
        String userTokenKey = TOKEN_PREFIX + userId;
        String tokenUserKey = USER_PREFIX + token;
        
        // 存储 user:token:{userId} -> token
        redisTemplate.opsForValue().set(userTokenKey, token, TOKEN_EXPIRE_TIME, TimeUnit.DAYS);
        
        // 存储 token:user:{token} -> userId (用于快速验证)
        redisTemplate.opsForValue().set(tokenUserKey, userId, TOKEN_EXPIRE_TIME, TimeUnit.DAYS);
        
        log.debug("Token已存储到Redis: userId={}", userId);
    }

    /**
     * 验证Token是否有效
     * @param token Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            // 首先验证JWT格式和签名
            if (!jwtUtil.isValidTokenFormat(token)) {
                log.warn("Token格式无效");
                return false;
            }

            // 检查Token是否过期
            if (jwtUtil.isTokenExpired(token)) {
                log.warn("Token已过期");
                removeTokenFromRedis(token);
                return false;
            }

            // 检查Token是否在Redis中存在
            String tokenUserKey = USER_PREFIX + token;
            Object userIdObj = redisTemplate.opsForValue().get(tokenUserKey);
            Long userId = null;
            if (userIdObj instanceof Number) {
                userId = ((Number) userIdObj).longValue();
            }

            if (userId == null) {
                log.warn("Token在Redis中不存在或已过期");
                return false;
            }

            // 验证JWT中的用户ID与Redis中的是否一致
            Long jwtUserId = jwtUtil.getUserIdFromToken(token);
            if (!userId.equals(jwtUserId)) {
                log.warn("Token中的用户ID与Redis中的不匹配");
                removeTokenFromRedis(token);
                return false;
            }

            log.debug("Token验证成功: userId={}", userId);
            return true;
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取Token对应的用户ID
     * @param token Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        if (!validateToken(token)) {
            return null;
        }

        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("从Token获取用户ID失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取用户当前的Token
     * @param userId 用户ID
     * @return Token，如果不存在则返回null
     */
    public String getUserToken(Long userId) {
        try {
            String userTokenKey = TOKEN_PREFIX + userId;
            return (String) redisTemplate.opsForValue().get(userTokenKey);
        } catch (Exception e) {
            log.error("获取用户Token失败: userId={}, error={}", userId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 刷新Token
     * @param oldToken 旧Token
     * @return 新Token
     */
    public String refreshToken(String oldToken) {
        try {
            // 验证旧Token
            if (!validateToken(oldToken)) {
                throw new IllegalArgumentException("旧Token无效");
            }

            // 获取用户信息
            Long userId = jwtUtil.getUserIdFromToken(oldToken);
            String username = jwtUtil.getUsernameFromToken(oldToken);

            if (userId == null || username == null) {
                throw new IllegalArgumentException("无法从旧Token获取用户信息");
            }

            // 删除旧Token
            removeTokenFromRedis(oldToken);

            // 生成新Token
            String newToken = generateAndStoreToken(userId, username);
            
            log.info("用户 {} Token刷新成功", userId);
            return newToken;
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage(), e);
            throw new RuntimeException("Token刷新失败", e);
        }
    }

    /**
     * 登出，删除Token
     * @param userId 用户ID
     */
    public void logout(Long userId) {
        try {
            String userToken = getUserToken(userId);
            if (userToken != null) {
                removeTokenFromRedis(userToken);
                log.info("用户 {} 登出成功，Token已清理", userId);
            } else {
                log.warn("用户 {} 登出时未找到对应Token", userId);
            }
        } catch (Exception e) {
            log.error("用户 {} 登出失败: {}", userId, e.getMessage(), e);
            throw new RuntimeException("登出失败", e);
        }
    }

    /**
     * 从Redis中删除Token
     * @param token Token
     */
    private void removeTokenFromRedis(String token) {
        try {
            // 获取用户ID
            String tokenUserKey = USER_PREFIX + token;
            Long userId = (Long) redisTemplate.opsForValue().get(tokenUserKey);

            // 删除两个键
            if (userId != null) {
                String userTokenKey = TOKEN_PREFIX + userId;
                redisTemplate.delete(userTokenKey);
            }
            redisTemplate.delete(tokenUserKey);
            
            log.debug("Token已从Redis中删除: userId={}", userId);
        } catch (Exception e) {
            log.error("从Redis删除Token失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查用户是否已登录
     * @param userId 用户ID
     * @return 是否已登录
     */
    public boolean isUserLoggedIn(Long userId) {
        String token = getUserToken(userId);
        return token != null && validateToken(token);
    }

    /**
     * 强制用户下线（清理所有Token）
     * @param userId 用户ID
     */
    public void forceLogout(Long userId) {
        try {
            logout(userId);
            log.info("用户 {} 被强制下线", userId);
        } catch (Exception e) {
            log.error("强制用户 {} 下线失败: {}", userId, e.getMessage(), e);
        }
    }
}