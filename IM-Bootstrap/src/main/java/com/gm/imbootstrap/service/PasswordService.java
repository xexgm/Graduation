package com.gm.imbootstrap.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author: xexgm
 * @date: 2025/10/1
 * desc: 密码加密服务
 */
@Slf4j
@Service
public class PasswordService {

    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.security.password-strength:8}")
    private int minPasswordLength;

    public PasswordService() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        validatePasswordStrength(rawPassword);
        
        String encodedPassword = passwordEncoder.encode(rawPassword);
        log.debug("密码加密成功");
        return encodedPassword;
    }

    /**
     * 验证密码是否匹配
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        log.debug("密码验证结果: {}", matches ? "成功" : "失败");
        return matches;
    }

    /**
     * 验证密码强度
     * @param password 密码
     */
    public void validatePasswordStrength(String password) {
        if (password == null || password.length() < minPasswordLength) {
            throw new IllegalArgumentException("密码长度不能少于 " + minPasswordLength + " 位");
        }

        // 检查是否包含数字
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("密码必须包含至少一个数字");
        }

        // 检查是否包含字母
        if (!password.matches(".*[a-zA-Z].*")) {
            throw new IllegalArgumentException("密码必须包含至少一个字母");
        }

        // 可以添加更多的密码强度检查
        // 例如：特殊字符、大小写字母等
        log.debug("密码强度验证通过");
    }

    /**
     * 检查密码是否需要重新加密（用于升级加密强度）
     * @param encodedPassword 已加密的密码
     * @return 是否需要重新加密
     */
    public boolean upgradeEncoding(String encodedPassword) {
        return passwordEncoder.upgradeEncoding(encodedPassword);
    }

    /**
     * 生成随机密码
     * @param length 密码长度
     * @return 随机密码
     */
    public String generateRandomPassword(int length) {
        if (length < minPasswordLength) {
            length = minPasswordLength;
        }

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();

        // 确保至少包含一个数字和一个字母
        password.append(chars.charAt((int) (Math.random() * 26))); // 大写字母
        password.append(chars.charAt((int) (Math.random() * 26) + 26)); // 小写字母
        password.append(chars.charAt((int) (Math.random() * 10) + 52)); // 数字

        // 填充剩余长度
        for (int i = 3; i < length; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }

        // 打乱字符顺序
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int index = (int) (Math.random() * (i + 1));
            char temp = passwordArray[index];
            passwordArray[index] = passwordArray[i];
            passwordArray[i] = temp;
        }

        return new String(passwordArray);
    }
}