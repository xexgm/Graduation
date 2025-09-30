package com.gm.graduation.netty.handler;

import com.gm.graduation.common.domain.CompleteMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author: xexgm
 * @date: 2025/9/29
 */
public class BusinessHandler extends SimpleChannelInboundHandler<CompleteMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, CompleteMessage completeMessage)
        throws Exception {

    }
}
