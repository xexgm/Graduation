package com.gm.graduation.common.enums;

/**
 * @author: xexgm
 * @date: 2025/9/30
 */
public enum UserRoleEnum {

    SIMPLE(0),  // 普通用户
    ADMIN(1);   // 管理员

    int role;

    UserRoleEnum(int role) {
        this.role = role;
    }
}
