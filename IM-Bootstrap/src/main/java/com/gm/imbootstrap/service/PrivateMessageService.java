package com.gm.imbootstrap.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gm.graduation.common.domain.PrivateMessage;
import com.gm.imbootstrap.mapper.PrivateMessageMapper;
import com.gm.graduation.common.api.IPrivateMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PrivateMessageService implements IPrivateMessageService {

    @Autowired
    private PrivateMessageMapper privateMessageMapper;

    @Override
    public void saveMessage(PrivateMessage message) {
        if (message != null) {
            privateMessageMapper.insert(message);
        }
    }

    /**
     * 获取私聊历史记录，按时间倒序排列（最新的在前面）
     */
    public Page<PrivateMessage> getHistory(Long userId, Long friendId, int current, int size) {
        Page<PrivateMessage> page = new Page<>(current, size);

        QueryWrapper<PrivateMessage> qw = new QueryWrapper<>();
        qw.and(wrapper -> wrapper
            .eq("sender_id", userId).eq("receiver_id", friendId)
            .or()
            .eq("sender_id", friendId).eq("receiver_id", userId)
        );
        qw.orderByDesc("create_time");

        return privateMessageMapper.selectPage(page, qw);
    }
}
