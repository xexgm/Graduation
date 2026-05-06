package com.gm.graduation.common.enums;

import lombok.Getter;

/**
 * @author: xexgm
 * @date: 2025/10/1
 * desc: WebSocket message types under each app line.
 */
@Getter
public enum MessageTypeEnum {

    /** Link app: establish connection. */
    LINK_CONNECT(AppEnum.LINK, 0),
    /** Link app: disconnect connection. */
    LINK_DISCONNECT(AppEnum.LINK, 1),
    /** Link app: heartbeat. */
    LINK_HEARTBEAT(AppEnum.LINK, 2),

    /** Chat room app: join room. */
    CHAT_ROOM_JOIN(AppEnum.CHAT_ROOM, 0),
    /** Chat room app: text message. */
    CHAT_ROOM_TEXT(AppEnum.CHAT_ROOM, 1),
    /** Chat room app: leave room. */
    CHAT_ROOM_LEAVE(AppEnum.CHAT_ROOM, 2),
    /** Chat room app: file message. */
    CHAT_ROOM_FILE(AppEnum.CHAT_ROOM, 3),
    /** Chat room app: audio message. */
    CHAT_ROOM_AUDIO(AppEnum.CHAT_ROOM, 4),

    /** Private chat app: text message. */
    PRIVATE_CHAT_TEXT(AppEnum.PRIVATE_CHAT, 1),
    /** Private chat app: file message. */
    PRIVATE_CHAT_FILE(AppEnum.PRIVATE_CHAT, 2),
    /** Private chat app: audio message. */
    PRIVATE_CHAT_AUDIO(AppEnum.PRIVATE_CHAT, 3);

    private final AppEnum app;

    private final int type;

    MessageTypeEnum(AppEnum app, int type) {
        this.app = app;
        this.type = type;
    }

    public static MessageTypeEnum fromAppAndType(Integer app, Integer type) {
        if (app == null || type == null) {
            return null;
        }
        for (MessageTypeEnum typeEnum : values()) {
            if (typeEnum.getApp().getApp().equals(app) && typeEnum.getType() == type) {
                return typeEnum;
            }
        }
        return null;
    }
}
