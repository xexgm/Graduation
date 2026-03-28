package com.gm.graduation.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * @author: xexgm
 * @date: 2025/9/30
 */
public enum ChatRoomTypeEnum {

    PUBLIC_ROOM(0, "公开"),
    PRIVATE_ROOM(1, "私有");

    @EnumValue
    private final Integer code;
    private final String type;

    ChatRoomTypeEnum(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public Integer getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}
