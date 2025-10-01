package com.gm.graduation.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 聊天室状态
 */
public enum ChatRoomStatusEnum {

    ACTIVE(0),     // 活跃
    DISBANDED(1),  // 下线(暂时无法进入)
    DELETED(2);    // 已删除

    @EnumValue
    private final Integer status;

    ChatRoomStatusEnum(int status) {
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }
}
