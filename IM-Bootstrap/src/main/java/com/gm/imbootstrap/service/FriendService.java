package com.gm.imbootstrap.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gm.graduation.common.domain.FriendRelation;
import com.gm.graduation.common.domain.User;
import com.gm.graduation.common.enums.FriendStatusEnum;
import com.gm.imbootstrap.dto.friend.FriendResponse;
import com.gm.imbootstrap.mapper.FriendRelationMapper;
import com.gm.imbootstrap.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FriendService {

    @Autowired
    private FriendRelationMapper friendRelationMapper;

    @Autowired
    private UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }

        // 检查目标好友是否存在
        QueryWrapper<User> userQw = new QueryWrapper<>();
        userQw.eq("user_id", friendId);
        User friend = userMapper.selectOne(userQw);
        if (friend == null) {
            throw new IllegalArgumentException("目标用户不存在");
        }

        // 检查是否已经是好友
        QueryWrapper<FriendRelation> relationQw = new QueryWrapper<>();
        relationQw.eq("user_id", userId).eq("friend_id", friendId);
        if (friendRelationMapper.selectCount(relationQw) > 0) {
            throw new IllegalArgumentException("该用户已经是你的好友");
        }

        // 这里仅作单向添加示例（若需双向好友关系，需要插入两条记录）
        // 目前先按照常规的双向处理插入两条记录
        FriendRelation relation1 = new FriendRelation();
        relation1.setUserId(userId);
        relation1.setFriendId(friendId);
        relation1.setStatus(FriendStatusEnum.NORMAL);
        relation1.setCreateTime(LocalDateTime.now());
        friendRelationMapper.insert(relation1);

        FriendRelation relation2 = new FriendRelation();
        relation2.setUserId(friendId);
        relation2.setFriendId(userId);
        relation2.setStatus(FriendStatusEnum.NORMAL);
        relation2.setCreateTime(LocalDateTime.now());
        friendRelationMapper.insert(relation2);

        log.info("用户 {} 和用户 {} 成功建立好友关系", userId, friendId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFriend(Long userId, Long friendId) {
        QueryWrapper<FriendRelation> qw1 = new QueryWrapper<>();
        qw1.eq("user_id", userId).eq("friend_id", friendId);
        friendRelationMapper.delete(qw1);

        QueryWrapper<FriendRelation> qw2 = new QueryWrapper<>();
        qw2.eq("user_id", friendId).eq("friend_id", userId);
        friendRelationMapper.delete(qw2);

        log.info("用户 {} 删除了好友 {}", userId, friendId);
    }

    public List<FriendResponse> listFriends(Long userId) {
        QueryWrapper<FriendRelation> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        List<FriendRelation> relations = friendRelationMapper.selectList(qw);

        return relations.stream().map(relation -> {
            FriendResponse resp = new FriendResponse();
            resp.setId(relation.getId());
            resp.setFriendId(relation.getFriendId());
            resp.setStatus(relation.getStatus());
            resp.setCreateTime(relation.getCreateTime());

            // 填充好友基本信息
            QueryWrapper<User> userQw = new QueryWrapper<>();
            userQw.eq("user_id", relation.getFriendId());
            User friend = userMapper.selectOne(userQw);
            if (friend != null) {
                resp.setUsername(friend.getUsername());
                resp.setNickname(friend.getNickname());
                resp.setAvatarUrl(friend.getAvatarUrl());
                resp.setSignature(friend.getSignature());
            }
            return resp;
        }).collect(Collectors.toList());
    }
}
