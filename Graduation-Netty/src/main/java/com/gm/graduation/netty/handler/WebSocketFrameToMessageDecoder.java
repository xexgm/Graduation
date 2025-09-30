package com.gm.graduation.netty.handler;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.graduation.common.domain.CompleteMessage;
import com.gm.graduation.common.utils.JsonUtil;
import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * @author: xexgm
 * @date: 2025/9/30
 */
public class WebSocketFrameToMessageDecoder extends MessageToMessageDecoder<TextWebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame msg, List<Object> out) throws Exception {
        String json = msg.text();

        if (json == null || json.isEmpty()) {
            return;
        }

        CompleteMessage completeMessage = JsonUtil.getObjectMapper().readValue(json, CompleteMessage.class);

        if (completeMessage != null) {
            out.add(completeMessage);
        }

    }
}
