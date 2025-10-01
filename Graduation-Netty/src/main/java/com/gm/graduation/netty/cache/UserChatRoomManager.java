package com.gm.graduation.netty.cache;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

/**
 * @author: xexgm
 * @date: 2025/9/30
 * desc: 管理各个聊天室所属的成员
 */
public class UserChatRoomManager {

    /** chatRoom -> Set<User> **/
    private static ConcurrentHashMap<Long, Set<Long>> chatRoom2User = new ConcurrentHashMap<>();

    /** 用户加入聊天室 **/
    public static boolean addChatRoomUser(Long roomId, Long userId) {
        return chatRoom2User.computeIfAbsent(roomId, k -> new HashSet<>()).add(userId);
    }

    /** 拿到聊天室的用户列表 **/
    public static Set<Long> getChatRoomUserSet(Long roomId) {
        return chatRoom2User.get(roomId);
    }

    /** 用户退出聊天室 **/
    public static boolean removeChatRoomUser(Long roomId, Long userId) {
        return Optional.ofNullable(chatRoom2User.get(roomId))
            .map(userSet -> userSet.remove(userId))
            .orElse(false);
    }


}
