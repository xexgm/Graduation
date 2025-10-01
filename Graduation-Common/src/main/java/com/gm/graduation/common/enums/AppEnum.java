package com.gm.graduation.common.enums;

import lombok.Getter;

/**
 * @author: xexgm
 * @date: 2025/9/30
 * desc: 业务线枚举，划分大体业务线：长连接层面、业务层面
 */
@Getter
public enum AppEnum {

    /** 基础能力 **/
    LINK(0),    // 连接中台业务，管理连接，心跳，等
    /** 以下为IM业务 **/
    CHAT_ROOM(1);

    Integer app;

    AppEnum(int num) {
        this.app = num;
    }

    public static AppEnum fromApp(int type) {
        for (AppEnum appEnum : values()) {
            if (appEnum.getApp() == type) {
                return appEnum;
            }
        }
        return null;
    }
}
