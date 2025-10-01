package com.gm.graduation.common.enums;

import lombok.Getter;

/**
 * @author: xexgm
 * @date: 2025/10/1
 * desc: todo 暂时没用
 */
@Getter
@Deprecated
public enum MessageTypeEnum {

    /**发送聊天室消息**/
    CHAT_ROOM(0);

    int type;

    MessageTypeEnum(int type) {
        this.type = type;
    }

    public static MessageTypeEnum fromType(int type) {
        for (MessageTypeEnum typeEnum : values()) {
            if (typeEnum.getType() == type) {
                return typeEnum;
            }
        }
        return null;
    }
}
