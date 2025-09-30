package com.gm.graduation.netty.handler;

import java.util.List;

import com.gm.graduation.common.domain.CompleteMessage;
import com.gm.graduation.common.utils.JsonUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * @author: xexgm
 * @date: 2025/9/30
 */
public class MessageToWebSocketFrameEncoder extends MessageToMessageDecoder<CompleteMessage> {

    @Override
    protected void decode(ChannelHandlerContext ctx, CompleteMessage completeMessage, List<Object> out) throws Exception {
        if (completeMessage == null) {
            return;
        }

        // 序列化为 json
        String json = JsonUtil.getObjectMapper().writeValueAsString(completeMessage);

        // 包装为帧
        TextWebSocketFrame frame = new TextWebSocketFrame(json);

        out.add(frame);
    }
}
