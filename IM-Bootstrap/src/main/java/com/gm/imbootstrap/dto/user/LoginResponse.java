package com.gm.imbootstrap.dto.user;

import com.gm.graduation.common.domain.User;
import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {
    private User user;
    private String token;
    private Long tokenExpireTime;

    public LoginResponse(User user, String token) {
        this.user = user;
        this.token = token;
        // 计算Token过期时间（7天后）
        this.tokenExpireTime = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
    }
}