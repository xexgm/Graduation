package com.gm.graduation.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author: xexgm
 * desc: 聊天室历史消息实体
 */
@Data
@TableName("graduation_chatroom_message")
public class ChatRoomMessage {

    @TableId(type = IdType.AUTO)
    private Long msgId;

    private Long roomId;

    private Long senderId;

    private String content;

    private LocalDateTime createTime;
}
