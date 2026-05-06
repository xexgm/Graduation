package com.gm.imbootstrap.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gm.graduation.common.api.IChatRoomMessageService;
import com.gm.graduation.common.domain.ChatRoomMessage;
import com.gm.imbootstrap.mapper.ChatRoomMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatRoomMessageService implements IChatRoomMessageService {

    @Autowired
    private ChatRoomMessageMapper chatRoomMessageMapper;

    @Autowired
    private MessageCryptoService messageCryptoService;

    @Override
    public void saveMessage(ChatRoomMessage message) {
        if (message != null) {
            String aad = buildAad(message.getRoomId(), message.getSenderId());
            message.setContent(messageCryptoService.encrypt(message.getContent(), aad));
            chatRoomMessageMapper.insert(message);
        }
    }

    /**
     * 获取聊天室历史记录，按时间倒序排列（最新的在前面）
     */
    public Page<ChatRoomMessage> getHistory(Long roomId, int current, int size) {
        Page<ChatRoomMessage> page = new Page<>(current, size);

        QueryWrapper<ChatRoomMessage> qw = new QueryWrapper<>();
        qw.eq("room_id", roomId);
        qw.orderByDesc("create_time");

        Page<ChatRoomMessage> result = chatRoomMessageMapper.selectPage(page, qw);
        result.getRecords().forEach(message -> {
            String aad = buildAad(message.getRoomId(), message.getSenderId());
            message.setContent(messageCryptoService.decrypt(message.getContent(), aad));
        });
        return result;
    }

    private String buildAad(Long roomId, Long senderId) {
        return "chatroom:" + roomId + ":" + senderId;
    }
}
