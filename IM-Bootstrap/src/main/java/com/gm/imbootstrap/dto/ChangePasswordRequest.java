package com.gm.imbootstrap.dto;

import lombok.Data;

/**
 * 修改密码请求DTO
 */
@Data
public class ChangePasswordRequest {
    private Long userId;
    private String oldPassword;
    private String newPassword;
}