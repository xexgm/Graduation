package com.gm.graduation.netty.processor;

import com.gm.graduation.common.domain.CompleteMessage;
import com.gm.graduation.netty.cache.UserChatRoomManager;
import com.gm.graduation.netty.cache.UserLinkManager;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * @author: xexgm
 * @date: 2025/10/1
 * desc: 聊天室处理器，包含 进入聊天室，发送聊天室消息，退出聊天室 等能力
 */
@Slf4j
public class ChatRoomProcessor extends AbstractMessageProcessor<CompleteMessage>{

    private ChatRoomProcessor(){}

    private static final ChatRoomProcessor INSTANCE = new ChatRoomProcessor();

    public static ChatRoomProcessor getInstance() {return INSTANCE;}

    /** 聊天室业务线处理方法，根据 msg的type，进行具体处理 **/
    public void process(ChannelHandlerContext ctx, CompleteMessage msg) {
        Integer messageType = msg.getMessageType();

        switch (messageType) {
            case 0:
                joinChatRoom(ctx, msg);
                break;
            case 1:
                sendChatRoomMessage(ctx, msg);
                break;
            case 2:
                leaveChatRoom(ctx, msg);
                break;
            default:
                log.warn("未知的聊天室消息类型: {}, userId: {}", messageType, msg.getUid());
        }
    }

    /** messageType为0，进入聊天室 **/
    private void joinChatRoom(ChannelHandlerContext ctx, CompleteMessage msg) {
        Long userId = msg.getUid();
        Long roomId = msg.getToId(); // 使用toId作为聊天室ID

        if (userId == null || roomId == null) {
            log.warn("进入聊天室参数不完整: userId={}, roomId={}", userId, roomId);
            return;
        }

        // 将用户添加到聊天室
        boolean success = UserChatRoomManager.addChatRoomUser(roomId, userId);
        
        // 确保用户的channel连接被管理
        UserLinkManager.addUserChannel(userId, ctx);

        if (success) {
            log.info("用户 {} 成功进入聊天室 {}", userId, roomId);
            
            // 可以向用户发送进入成功的确认消息
            CompleteMessage response = new CompleteMessage();
            response.setAppId(msg.getAppId());
            response.setUid(userId);
            response.setMessageType(0);
            response.setToId(roomId);
            response.setContent("成功进入聊天室: " + roomId);
            response.setTimeStamp(System.currentTimeMillis());
            
            ctx.writeAndFlush(response);
        } else {
            log.warn("用户 {} 进入聊天室 {} 失败", userId, roomId);
        }
    }

    /** messageType为1，发送聊天室消息 **/
    private void sendChatRoomMessage(ChannelHandlerContext ctx, CompleteMessage msg) {
        Long senderId = msg.getUid();
        Long roomId = msg.getToId(); // 使用toId作为聊天室ID
        String content = msg.getContent();

        if (senderId == null || roomId == null || content == null) {
            log.warn("发送聊天室消息参数不完整: senderId={}, roomId={}, content={}", senderId, roomId, content);
            return;
        }

        // 获取聊天室中的所有用户
        Set<Long> userSet = UserChatRoomManager.getChatRoomUserSet(roomId);
        
        if (userSet == null || userSet.isEmpty()) {
            log.warn("聊天室 {} 中没有用户", roomId);
            return;
        }

        log.info("向聊天室 {} 中的 {} 个用户广播消息，发送者: {}", roomId, userSet.size(), senderId);

        // 构建要广播的消息
        CompleteMessage broadcastMsg = new CompleteMessage();
        broadcastMsg.setAppId(msg.getAppId());
        broadcastMsg.setUid(senderId);
        broadcastMsg.setMessageType(1);
        broadcastMsg.setToId(roomId);
        broadcastMsg.setContent(content);
        broadcastMsg.setTimeStamp(System.currentTimeMillis());

        // 向聊天室中的每个用户发送消息
        int successCount = 0;
        for (Long userId : userSet) {
            ChannelHandlerContext userCtx = UserLinkManager.getUserChannelCtx(userId);
            if (userCtx != null && userCtx.channel().isActive()) {
                try {
                    userCtx.writeAndFlush(broadcastMsg);
                    successCount++;
                } catch (Exception e) {
                    log.error("向用户 {} 发送消息失败", userId, e);
                }
            } else {
                log.warn("用户 {} 的连接已断开或不可用", userId);
                // 可以考虑从聊天室中移除该用户
                UserChatRoomManager.removeChatRoomUser(roomId, userId);
            }
        }

        log.info("聊天室 {} 消息广播完成，成功发送给 {} 个用户", roomId, successCount);
    }

    /** messageType为2，退出聊天室 **/
    private void leaveChatRoom(ChannelHandlerContext ctx, CompleteMessage msg) {
        Long userId = msg.getUid();
        Long roomId = msg.getToId(); // 使用toId作为聊天室ID

        if (userId == null || roomId == null) {
            log.warn("退出聊天室参数不完整: userId={}, roomId={}", userId, roomId);
            return;
        }

        // 从聊天室中移除用户
        boolean success = UserChatRoomManager.removeChatRoomUser(roomId, userId);

        if (success) {
            log.info("用户 {} 成功退出聊天室 {}", userId, roomId);
            
            // 向用户发送退出成功的确认消息
            CompleteMessage response = new CompleteMessage();
            response.setAppId(msg.getAppId());
            response.setUid(userId);
            response.setMessageType(2);
            response.setToId(roomId);
            response.setContent("成功退出聊天室: " + roomId);
            response.setTimeStamp(System.currentTimeMillis());
            
            ctx.writeAndFlush(response);
        } else {
            log.warn("用户 {} 退出聊天室 {} 失败（可能用户不在该聊天室中）", userId, roomId);
        }
    }
}
