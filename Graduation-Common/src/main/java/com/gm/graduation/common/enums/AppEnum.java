package com.gm.graduation.common.enums;

/**
 * @author: xexgm
 * @date: 2025/9/30
 * desc: 业务线枚举
 */
public enum AppEnum {

    LINK(0),    // 连接中台业务，管理连接，心跳，等
    IM_SERVER(1);    // IM业务，聊天室，等

    Integer num;

    AppEnum(int num) {
        this.num = num;
    }
}
