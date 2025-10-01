package com.gm.imbootstrap.dto.user;

import lombok.Data;

/**
 * 用于接收登录请求的DTO
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}