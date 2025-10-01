package com.gm.graduation.common.domain;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gm.graduation.common.enums.ChatRoomStatusEnum;
import com.gm.graduation.common.enums.ChatRoomTypeEnum;
import lombok.Data;

/**
 * @author: xexgm
 * @date: 2025/9/30
 */
@Data
@TableName("graduation_chat_room")
public class ChatRoom {

    /**
     * 聊天室id
     */
    @TableId(type = IdType.AUTO)
    Long roomId;

    /**
     * 聊天室名称
     */
    String roomName;

    /**
     * 聊天室公告
     */
    String description;

    /**
     * 聊天室创建者
     */
    Long ownerId;

    /**
     * 聊天室类型
     */
    ChatRoomTypeEnum roomType;

    /**
     * 聊天室创建时间
     */
    LocalDateTime createTime;

    /**
     * 聊天室状态
     */
    ChatRoomStatusEnum status;
}
