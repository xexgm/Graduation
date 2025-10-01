package com.gm.graduation.netty.handler;

import com.gm.graduation.common.domain.CompleteMessage;
import com.gm.graduation.common.enums.AppEnum;
import com.gm.graduation.netty.processor.AbstractMessageProcessor;
import com.gm.graduation.netty.processor.ProcessorFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

/**
 * @author: xexgm
 * @date: 2025/9/29
 */
public class BusinessHandler extends SimpleChannelInboundHandler<CompleteMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, CompleteMessage completeMessage) throws Exception {
        // 根据 appid 划分业务处理器
        // 再根据 messageType 划分具体处理场景
        AppEnum app = AppEnum.fromApp(completeMessage.getAppId());

        if (app == null) {
            // 在读的中途停止了传递，手动释放 ByteBuf，避免内存泄漏
            ReferenceCountUtil.release(completeMessage);
            return;
        }

        AbstractMessageProcessor<CompleteMessage> processor = ProcessorFactory.getProcessor(app);
        processor.process(channelHandlerContext, completeMessage);
    }
}
