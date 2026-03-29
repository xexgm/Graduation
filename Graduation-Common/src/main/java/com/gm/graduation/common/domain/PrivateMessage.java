package com.gm.graduation.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author: xexgm
 * desc: 私聊消息实体
 */
@Data
@TableName("graduation_private_message")
public class PrivateMessage {

    @TableId(type = IdType.AUTO)
    private Long msgId;

    private Long senderId;

    private Long receiverId;

    private String content;

    private Integer isRead;

    private LocalDateTime createTime;
}
