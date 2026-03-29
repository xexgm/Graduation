package com.gm.graduation.netty.processor;

import com.gm.graduation.common.api.IPrivateMessageService;
import com.gm.graduation.common.domain.CompleteMessage;
import com.gm.graduation.common.domain.PrivateMessage;
import com.gm.graduation.netty.cache.UserLinkManager;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * @author: xexgm
 * desc: 私聊消息处理器
 */
@Slf4j
public class PrivateChatProcessor extends AbstractMessageProcessor<CompleteMessage> {

    private static IPrivateMessageService privateMessageService;

    public static void setPrivateMessageService(IPrivateMessageService service) {
        privateMessageService = service;
    }

    private PrivateChatProcessor() {}

    private static final PrivateChatProcessor INSTANCE = new PrivateChatProcessor();

    public static PrivateChatProcessor getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(ChannelHandlerContext ctx, CompleteMessage msg) {
        Long senderId = msg.getUid();
        Long receiverId = msg.getToId();
        String content = msg.getContent();

        if (senderId == null || receiverId == null || content == null) {
            log.warn("私聊消息参数不完整: senderId={}, receiverId={}, content={}", senderId, receiverId, content);
            return;
        }

        // 1. 构建私聊实体并落库
        PrivateMessage privateMessage = new PrivateMessage();
        privateMessage.setSenderId(senderId);
        privateMessage.setReceiverId(receiverId);
        privateMessage.setContent(content);
        privateMessage.setCreateTime(LocalDateTime.now());
        
        ChannelHandlerContext receiverCtx = UserLinkManager.getUserChannelCtx(receiverId);
        if (receiverCtx != null && receiverCtx.channel().isActive()) {
            privateMessage.setIsRead(1); // 标记为已读（或者由前端后续ACK，这里简单处理）
        } else {
            privateMessage.setIsRead(0); // 标记为未读（离线消息）
        }

        if (privateMessageService != null) {
            try {
                privateMessageService.saveMessage(privateMessage);
            } catch (Exception e) {
                log.error("私聊消息落库失败", e);
            }
        } else {
            log.warn("PrivateMessageService 未注入，消息无法落库");
        }

        // 2. 尝试在线推送
        if (receiverCtx != null && receiverCtx.channel().isActive()) {
            CompleteMessage pushMsg = new CompleteMessage();
            pushMsg.setAppId(msg.getAppId());
            pushMsg.setUid(senderId);
            pushMsg.setToId(receiverId);
            pushMsg.setMessageType(msg.getMessageType());
            pushMsg.setContent(content);
            pushMsg.setTimeStamp(System.currentTimeMillis());

            receiverCtx.writeAndFlush(pushMsg);
            log.info("用户 {} 向 用户 {} 发送私聊消息，已实时推送", senderId, receiverId);
        } else {
            log.info("用户 {} 向 用户 {} 发送私聊消息，对方离线，已转为离线消息", senderId, receiverId);
        }
    }
}
