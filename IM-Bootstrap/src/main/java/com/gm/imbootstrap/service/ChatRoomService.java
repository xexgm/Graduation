package com.gm.imbootstrap.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gm.graduation.common.domain.ChatRoom;
import com.gm.graduation.common.domain.User;
import com.gm.graduation.common.enums.ChatRoomStatusEnum;
import com.gm.graduation.common.enums.ChatRoomTypeEnum;
import com.gm.imbootstrap.mapper.ChatRoomMapper;
import com.gm.imbootstrap.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ChatRoomService {

    @Autowired
    private ChatRoomMapper chatRoomMapper;

    @Autowired
    private UserMapper userMapper;

    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        try {
            // 避免 User 主键注解缺失导致的 selectById 问题，走列查询
            QueryWrapper<User> qw = new QueryWrapper<>();
            qw.eq("user_id", userId);
            User user = userMapper.selectOne(qw);
            return user != null && user.getRole() != null && user.getRole() == 1;
        } catch (Exception e) {
            log.error("检查管理员权限失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatRoom create(Long operatorUserId, String roomName, String description, ChatRoomTypeEnum roomType) throws Exception {
        if (!isAdmin(operatorUserId)) {
            throw new IllegalAccessException("仅管理员可操作");
        }
        if (roomName == null || roomName.trim().isEmpty()) {
            throw new IllegalArgumentException("聊天室名称不能为空");
        }

        ChatRoom room = new ChatRoom();
        room.setRoomName(roomName.trim());
        room.setDescription(description);
        room.setOwnerId(operatorUserId);
        room.setRoomType(roomType == null ? ChatRoomTypeEnum.PUBLIC_ROOM : roomType);
        room.setCreateTime(LocalDateTime.now());
        room.setStatus(ChatRoomStatusEnum.ACTIVE);

        chatRoomMapper.insert(room);
        log.info("聊天室创建成功: roomId={}, name={}, operator={}", room.getRoomId(), room.getRoomName(), operatorUserId);
        return room;
    }

    @Transactional(rollbackFor = Exception.class)
    public void offline(Long operatorUserId, Long roomId) throws Exception {
        if (!isAdmin(operatorUserId)) {
            throw new IllegalAccessException("仅管理员可操作");
        }
        if (roomId == null) {
            throw new IllegalArgumentException("roomId不能为空");
        }

        ChatRoom update = new ChatRoom();
        update.setRoomId(roomId);
        update.setStatus(ChatRoomStatusEnum.DISBANDED);
        int cnt = chatRoomMapper.updateById(update);
        if (cnt <= 0) {
            throw new IllegalStateException("聊天室不存在或更新失败");
        }
        log.info("聊天室下线成功: roomId={}, operator={}", roomId, operatorUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long operatorUserId, Long roomId) throws Exception {
        if (!isAdmin(operatorUserId)) {
            throw new IllegalAccessException("仅管理员可操作");
        }
        if (roomId == null) {
            throw new IllegalArgumentException("roomId不能为空");
        }

        ChatRoom update = new ChatRoom();
        update.setRoomId(roomId);
        update.setStatus(ChatRoomStatusEnum.DELETED);
        int cnt = chatRoomMapper.updateById(update);
        if (cnt <= 0) {
            throw new IllegalStateException("聊天室不存在或更新失败");
        }
        log.info("聊天室删除成功(软删): roomId={}, operator={}", roomId, operatorUserId);
    }

    public List<ChatRoom> listAllNotDeleted() {
        QueryWrapper<ChatRoom> qw = new QueryWrapper<>();
        // 过滤已删除
        qw.ne("status", ChatRoomStatusEnum.DELETED);
        return chatRoomMapper.selectList(qw);
    }
}

