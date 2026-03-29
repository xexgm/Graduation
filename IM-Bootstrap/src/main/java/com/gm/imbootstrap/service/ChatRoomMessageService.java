package com.gm.imbootstrap.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gm.graduation.common.domain.ChatRoomMessage;
import com.gm.imbootstrap.mapper.ChatRoomMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatRoomMessageService {

    @Autowired
    private ChatRoomMessageMapper chatRoomMessageMapper;

    /**
     * 获取聊天室历史记录，按时间倒序排列（最新的在前面）
     */
    public Page<ChatRoomMessage> getHistory(Long roomId, int current, int size) {
        Page<ChatRoomMessage> page = new Page<>(current, size);

        QueryWrapper<ChatRoomMessage> qw = new QueryWrapper<>();
        qw.eq("room_id", roomId);
        qw.orderByDesc("create_time");

        return chatRoomMessageMapper.selectPage(page, qw);
    }
}
