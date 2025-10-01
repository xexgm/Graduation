package com.gm.imbootstrap.dto;

import lombok.Data;

/**
 * 用于接收登录请求的DTO
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}