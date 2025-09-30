package com.gm.graduation.common.domain;

/**
 * @author: xexgm
 * @date: 2025/9/29
 */
public class CompleteMessage {

    /**
     * 业务线id
     */
    int appId;

    /**
     * 用户id
     */
    long uid;

    /**
     * 用户 token
     */
    String token;

    /**
     * 是否压缩
     */
    int compression;

    /**
     * 是否加密
     */
    int encryption;

    /**
     * 消息类型
     */
    int messageType;


    long toId;

    String content;

    long timeStamp;

}
