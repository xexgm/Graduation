package com.gm.graduation.common.enums;

/**
 * @author: xexgm
 * @date: 2025/9/30
 */
public enum ChatRoomTypeEnum {

    PUBLIC_ROOM("公开"),
    PRIVATE_ROOM("私有");

    String type;

    ChatRoomTypeEnum(String type) {
        this.type = type;
    }
}
