package com.gm.graduation.common.domain;

import lombok.Data;

/**
 * @author: xexgm
 * @date: 2025/9/30
 */
@Data
public class User {

    /**
     * 用户id
     */
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
    Long createTime;

    /**
     * 用户更新时间
     */
    Long updateTime;
}
