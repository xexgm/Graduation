package com.gm.graduation.common.api;

import com.gm.graduation.common.domain.PrivateMessage;

/**
 * 暴露给Netty使用的私聊消息接口
 */
public interface IPrivateMessageService {
    void saveMessage(PrivateMessage message);
}
