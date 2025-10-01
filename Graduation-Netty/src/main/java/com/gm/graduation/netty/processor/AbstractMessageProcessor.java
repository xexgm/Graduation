package com.gm.graduation.netty.processor;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author: xexgm
 * @date: 2025/10/1
 */
public abstract class AbstractMessageProcessor<T> {

    public abstract void process(ChannelHandlerContext ctx, T msg);

}
