package com.gm.graduation.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gm.graduation.common.enums.FriendStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author: xexgm
 * desc: 用户好友关系实体
 */
@Data
@TableName("graduation_friend_relation")
public class FriendRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long friendId;

    private FriendStatusEnum status;

    private LocalDateTime createTime;
}
