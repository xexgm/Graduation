package com.gm.graduation.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * @author: xexgm
 * desc: 好友状态枚举
 */
public enum FriendStatusEnum {

    NORMAL(0, "正常"),
    BLOCKED(1, "拉黑");

    @EnumValue
    private final Integer code;
    private final String status;

    FriendStatusEnum(int code, String status) {
        this.code = code;
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }
}
