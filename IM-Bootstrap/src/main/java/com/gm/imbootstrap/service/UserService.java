package com.gm.imbootstrap.service;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gm.graduation.common.domain.User;
import com.gm.imbootstrap.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: Gemini
 * @date: 2025/9/29
 * des: 用户服务
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private TokenService tokenService;

    /**
     * 用户注册
     *
     * @param userToRegister 包含用户名和密码的用户对象
     * @return 注册成功的用户对象
     * @throws Exception 如果用户名已存在或注册失败
     */
    @Transactional(rollbackFor = Exception.class)
    public User register(User userToRegister) throws Exception {
        // 1. 参数校验
        if (userToRegister == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        String username = userToRegister.getUsername();
        String password = userToRegister.getPassword();

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 2. 检查用户名是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username.trim());
        if (userMapper.selectOne(queryWrapper) != null) {
            throw new Exception("用户名 '" + username + "' 已存在");
        }

        // 3. 密码加密
        try {
            String encodedPassword = passwordService.encodePassword(password);
            userToRegister.setPassword(encodedPassword);
        } catch (Exception e) {
            throw new Exception("密码格式不符合要求: " + e.getMessage());
        }

        // 4. 设置默认值
        userToRegister.setUsername(username.trim());
        userToRegister.setCreateTime(LocalDateTime.now());
        userToRegister.setUpdateTime(LocalDateTime.now());
        userToRegister.setStatus(1); // 1表示正常状态

        // 设置默认昵称
        if (userToRegister.getNickname() == null || userToRegister.getNickname().trim().isEmpty()) {
            userToRegister.setNickname(username.trim());
        }

        // 5. 插入用户到数据库
        try {
            userMapper.insert(userToRegister);
            log.info("用户注册成功: userId={}, username={}", userToRegister.getUserId(), username);
            return userToRegister;
        } catch (Exception e) {
            log.error("用户注册失败: username={}, error={}", username, e.getMessage(), e);
            throw new Exception("注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录信息，包含用户信息和Token
     * @throws Exception 如果用户不存在或密码错误
     */
    public LoginResult login(String username, String password) throws Exception {
        // 1. 参数校验
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 2. 查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username.trim());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new Exception("用户不存在");
        }

        // 3. 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new Exception("用户账号已被禁用");
        }

        // 4. 验证密码
        if (!passwordService.matches(password, user.getPassword())) {
            throw new Exception("密码错误");
        }

        // 5. 检查是否已经登录（可选：强制下线旧会话）
        if (tokenService.isUserLoggedIn(user.getUserId())) {
            log.warn("用户 {} 重复登录，将强制下线旧会话", user.getUserId());
            tokenService.forceLogout(user.getUserId());
        }

        // 6. 生成Token
        String token = tokenService.generateAndStoreToken(user.getUserId(), user.getUsername());

        // 7. 返回登录结果
        log.info("用户登录成功: userId={}, username={}", user.getUserId(), username);
        return new LoginResult(user, token);
    }

    /**
     * 用户登出
     *
     * @param userId 要登出的用户ID
     */
    public void logout(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        try {
            tokenService.logout(userId);
            log.info("用户登出成功: userId={}", userId);
        } catch (Exception e) {
            log.error("用户登出失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("登出失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public User getUserById(Long userId) {
        if (userId == null) {
            return null;
        }

        try {
            User user = userMapper.selectById(userId);
            if (user != null) {
                // 移除密码信息
                user.setPassword(null);
            }
            return user;
        } catch (Exception e) {
            log.error("获取用户信息失败: userId={}, error={}", userId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    public User getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        try {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", username.trim());
            User user = userMapper.selectOne(queryWrapper);
            if (user != null) {
                // 移除密码信息
                user.setPassword(null);
            }
            return user;
        } catch (Exception e) {
            log.error("获取用户信息失败: username={}, error={}", username, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 验证Token并获取用户信息
     *
     * @param token Token
     * @return 用户信息，如果Token无效则返回null
     */
    public User validateTokenAndGetUser(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            Long userId = tokenService.getUserIdFromToken(token);
            if (userId != null) {
                return getUserById(userId);
            }
            return null;
        } catch (Exception e) {
            log.error("验证Token并获取用户信息失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @throws Exception 如果旧密码错误或新密码不符合要求
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) throws Exception {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        // 1. 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new Exception("用户不存在");
        }

        // 2. 验证旧密码
        if (!passwordService.matches(oldPassword, user.getPassword())) {
            throw new Exception("旧密码错误");
        }

        // 3. 加密新密码
        String encodedNewPassword = passwordService.encodePassword(newPassword);

        // 4. 更新密码
        user.setPassword(encodedNewPassword);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 5. 强制下线，要求重新登录
        tokenService.forceLogout(userId);

        log.info("用户修改密码成功: userId={}", userId);
    }

    /**
     * 登录结果类
     */
    public static class LoginResult {
        private User user;
        private String token;

        public LoginResult(User user, String token) {
            this.user = user;
            this.token = token;
            // 确保不返回密码
            if (this.user != null) {
                this.user.setPassword(null);
            }
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
