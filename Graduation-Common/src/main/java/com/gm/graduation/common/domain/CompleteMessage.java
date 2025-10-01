package com.gm.graduation.common.domain;

import lombok.Data;

/**
 * @author: xexgm
 * @date: 2025/9/29
 */
@Data
public class CompleteMessage {

    /**
     * 业务线id
     */
    Integer appId;

    /**
     * 用户id
     */
    Long uid;

    /**
     * 用户 token
     */
    String token;

    /**
     * 是否压缩
     */
    Integer compression;

    /**
     * 是否加密
     */
    Integer encryption;

    /**
     * 消息类型
     */
    Integer messageType;

    /**
     * 接收方id
     */
    Long toId;

    /**
     * 消息内容
     */
    String content;

    /**
     * 发送时间戳
     */
    Long timeStamp;

}
