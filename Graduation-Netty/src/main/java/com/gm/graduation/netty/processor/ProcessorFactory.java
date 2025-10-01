package com.gm.graduation.netty.processor;

import com.gm.graduation.common.domain.CompleteMessage;
import com.gm.graduation.common.enums.AppEnum;

/**
 * @author: xexgm
 * @date: 2025/10/1
 */
public class ProcessorFactory {

    public static AbstractMessageProcessor<CompleteMessage> getProcessor(AppEnum app) {
        return switch (app) {
            case CHAT_ROOM -> ChatRoomProcessor.getInstance();
            default -> throw new IllegalArgumentException("不支持的消息类型");
        };
    }
}
