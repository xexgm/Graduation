package com.gm.graduation.netty.cache;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: xexgm
 * @date: 2025/10/1
 */
@Slf4j
public class UserLinkManager {

    /** user -> channel **/
    private static ConcurrentHashMap<Long, ChannelHandlerContext> user2Channel = new ConcurrentHashMap<>();

    /** 添加 user -> channelCtx **/
    public static void addUserChannel(Long userId, ChannelHandlerContext channelHandlerContext) {
        if (userId == null || channelHandlerContext == null) {
            return;
        }

        user2Channel.put(userId, channelHandlerContext);
    }

    /** 删除 user -> channelCtx **/
    public static void removeUserChannel(Long userId) {
        if (userId != null) {
            user2Channel.remove(userId);
        }
    }

    /** 获取 user -> channelCtx **/
    public static ChannelHandlerContext getUserChannelCtx(Long userId) {
        return user2Channel.get(userId);
    }

    /** 根据channel移除对应的用户连接 **/
    public static void removeChannelFromAllUsers(ChannelHandlerContext ctx) {
        if (ctx == null) {
            return;
        }

        // 遍历所有用户连接，找到对应的channel并移除
        user2Channel.entrySet().removeIf(entry -> {
            ChannelHandlerContext userCtx = entry.getValue();
            if (userCtx != null && userCtx.equals(ctx)) {
                Long userId = entry.getKey();
                log.info("检测到用户 {} 的连接已断开，从连接管理器中移除", userId);
                return true;
            }
            return false;
        });
    }

    /** 获取当前在线用户数量 **/
    public static int getOnlineUserCount() {
        return user2Channel.size();
    }

    /** 清理所有无效连接 **/
    public static void cleanInactiveConnections() {
        user2Channel.entrySet().removeIf(entry -> {
            ChannelHandlerContext ctx = entry.getValue();
            if (ctx == null || !ctx.channel().isActive()) {
                Long userId = entry.getKey();
                log.info("清理用户 {} 的无效连接", userId);
                return true;
            }
            return false;
        });
    }
}
