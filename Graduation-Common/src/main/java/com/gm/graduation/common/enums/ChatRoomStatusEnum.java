package com.gm.graduation.common.enums;

/**
 * @author: xexgm
 * @date: 2025/9/30
 */
public enum ChatRoomStatusEnum {

    ACTIVE(0),  // 活跃
    ARCHIVED(1),    // 已归档(只读)
    DISBANDED(2);   // 下线

    Integer status;

    ChatRoomStatusEnum(int status) {
        this.status = status;
    }

}
