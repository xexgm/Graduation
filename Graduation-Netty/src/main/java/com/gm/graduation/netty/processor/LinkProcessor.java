package com.gm.graduation.netty.processor;

import com.gm.graduation.common.domain.CompleteMessage;
import com.gm.graduation.netty.cache.UserLinkManager;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: xexgm
 * @date: 2025/10/1
 * desc: 长连接处理器，包含 建连(0)、断线(1)、心跳(2)
 */
@Slf4j
public class LinkProcessor extends AbstractMessageProcessor<CompleteMessage>{

    private LinkProcessor(){}

    private static final LinkProcessor INSTANCE = new LinkProcessor();

    public static LinkProcessor getInstance() {return INSTANCE;}

    @Override
    public void process(ChannelHandlerContext ctx, CompleteMessage msg) {
        Integer messageType = msg.getMessageType();

        switch (messageType) {
            case 0:
                establishConnection(ctx, msg);
                break;
            case 1:
                disconnectConnection(ctx, msg);
                break;
            case 2:
                handleHeartbeat(ctx, msg);
                break;
            default:
                log.warn("未知的连接消息类型: {}, userId: {}", messageType, msg.getUid());
        }
    }

    /** messageType为0，建立连接 **/
    private void establishConnection(ChannelHandlerContext ctx, CompleteMessage msg) {
        Long userId = msg.getUid();
        String token = msg.getToken();

        if (userId == null) {
            log.warn("建立连接参数不完整: userId={}", userId);
            sendErrorResponse(ctx, msg, "用户ID不能为空");
            return;
        }

        // TODO: 这里可以添加token验证逻辑
        if (token == null || token.trim().isEmpty()) {
            log.warn("建立连接失败，token为空: userId={}", userId);
            sendErrorResponse(ctx, msg, "token不能为空");
            return;
        }

        // 检查用户是否已经连接
        ChannelHandlerContext existingCtx = UserLinkManager.getUserChannelCtx(userId);
        if (existingCtx != null && existingCtx.channel().isActive()) {
            log.warn("用户 {} 已经存在活跃连接，将断开旧连接", userId);
            // 断开旧连接
            existingCtx.close();
        }

        // 建立新连接
        UserLinkManager.addUserChannel(userId, ctx);
        
        log.info("用户 {} 成功建立连接, channel: {}", userId, ctx.channel().id().asShortText());

        // 发送连接成功响应
        CompleteMessage response = new CompleteMessage();
        response.setAppId(msg.getAppId());
        response.setUid(userId);
        response.setMessageType(0);
        response.setContent("连接建立成功");
        response.setTimeStamp(System.currentTimeMillis());
        
        ctx.writeAndFlush(response);
    }

    /** messageType为1，断开连接 **/
    private void disconnectConnection(ChannelHandlerContext ctx, CompleteMessage msg) {
        Long userId = msg.getUid();

        if (userId == null) {
            log.warn("断开连接参数不完整: userId={}", userId);
            return;
        }

        // 从连接管理器中移除用户
        UserLinkManager.removeUserChannel(userId);
        
        log.info("用户 {} 主动断开连接, channel: {}", userId, ctx.channel().id().asShortText());

        // 发送断开连接确认响应
        CompleteMessage response = new CompleteMessage();
        response.setAppId(msg.getAppId());
        response.setUid(userId);
        response.setMessageType(1);
        response.setContent("连接已断开");
        response.setTimeStamp(System.currentTimeMillis());
        
        // 发送响应后关闭连接
        ctx.writeAndFlush(response).addListener(future -> {
            ctx.close();
            log.info("用户 {} 连接已关闭", userId);
        });
    }

    /** messageType为2，心跳保活 **/
    private void handleHeartbeat(ChannelHandlerContext ctx, CompleteMessage msg) {
        Long userId = msg.getUid();

        if (userId == null) {
            log.warn("心跳消息参数不完整: userId={}", userId);
            return;
        }

        // 验证用户连接是否存在且活跃
        ChannelHandlerContext userCtx = UserLinkManager.getUserChannelCtx(userId);
        if (userCtx == null || !userCtx.channel().isActive()) {
            log.warn("用户 {} 心跳检测失败，连接不存在或已断开", userId);
            // 如果连接不存在，重新建立连接映射
            UserLinkManager.addUserChannel(userId, ctx);
        }

        log.debug("收到用户 {} 的心跳消息, channel: {}", userId, ctx.channel().id().asShortText());

        // 发送心跳响应
        CompleteMessage response = new CompleteMessage();
        response.setAppId(msg.getAppId());
        response.setUid(userId);
        response.setMessageType(2);
        response.setContent("pong");
        response.setTimeStamp(System.currentTimeMillis());
        
        ctx.writeAndFlush(response);
    }

    /** 发送错误响应 **/
    private void sendErrorResponse(ChannelHandlerContext ctx, CompleteMessage originalMsg, String errorMsg) {
        CompleteMessage response = new CompleteMessage();
        response.setAppId(originalMsg.getAppId());
        response.setUid(originalMsg.getUid());
        response.setMessageType(originalMsg.getMessageType());
        response.setContent("ERROR: " + errorMsg);
        response.setTimeStamp(System.currentTimeMillis());
        
        ctx.writeAndFlush(response);
    }

    /** 处理连接异常断开 **/
    public void handleChannelInactive(ChannelHandlerContext ctx) {
        // 当channel意外断开时，清理用户连接
        String channelId = ctx.channel().id().asShortText();
        log.info("检测到连接断开, channel: {}", channelId);
        
        // 遍历查找并移除对应的用户连接
        // 注意：这种方式效率不高，如果用户量大可以考虑维护channel到userId的反向映射
        UserLinkManager.removeChannelFromAllUsers(ctx);
    }
}
