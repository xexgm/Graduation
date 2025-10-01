package com.gm.graduation.common.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


/**
 * @author: xexgm
 * @date: 2025/9/30
 */
@Data
@TableName("graduation_user")
public class User {

    /**
     * 用户id
     */
    @TableId(type = IdType.AUTO)
    Long userId;

    /**
     * 用户名
     */
    String username;

    /**
     * 用户昵称
     */
    String nickname;

    /**
     * 密码，加密
     */
    String password;

    /**
     * 角色：0-普通用户，1-管理员
     */
    Integer role;

    /**
     * 头像链接
     */
    String avatarUrl;

    /**
     * 个性签名
     */
    String signature;

    /**
     * 账号状态
     */
    Integer status;

    /**
     * 用户注册时间
     */
    LocalDateTime createTime;

    /**
     * 用户更新时间
     */
    LocalDateTime updateTime;
}
