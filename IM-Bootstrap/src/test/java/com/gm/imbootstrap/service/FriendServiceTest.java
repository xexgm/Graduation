package com.gm.imbootstrap.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gm.graduation.common.domain.FriendRelation;
import com.gm.graduation.common.domain.User;
import com.gm.graduation.common.enums.FriendStatusEnum;
import com.gm.imbootstrap.dto.friend.FriendResponse;
import com.gm.imbootstrap.mapper.FriendRelationMapper;
import com.gm.imbootstrap.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @InjectMocks
    private FriendService friendService;

    @Mock
    private FriendRelationMapper friendRelationMapper;

    @Mock
    private UserMapper userMapper;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(2L);
        mockUser.setUsername("testUser");
        mockUser.setNickname("Test Nickname");
    }

    @Test
    void addFriend_Success() {
        // Arrange
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(mockUser);
        when(friendRelationMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(friendRelationMapper.insert(any(FriendRelation.class))).thenReturn(1);

        // Act
        friendService.addFriend(1L, 2L);

        // Assert
        verify(userMapper, times(1)).selectOne(any(QueryWrapper.class));
        verify(friendRelationMapper, times(1)).selectCount(any(QueryWrapper.class));
        verify(friendRelationMapper, times(2)).insert(any(FriendRelation.class));
    }

    @Test
    void addFriend_SameUser_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> friendService.addFriend(1L, 1L));
        assertEquals("不能添加自己为好友", exception.getMessage());
    }

    @Test
    void addFriend_TargetUserNotExist_ThrowsException() {
        // Arrange
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> friendService.addFriend(1L, 2L));
        assertEquals("目标用户不存在", exception.getMessage());
    }

    @Test
    void addFriend_AlreadyFriends_ThrowsException() {
        // Arrange
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(mockUser);
        when(friendRelationMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> friendService.addFriend(1L, 2L));
        assertEquals("该用户已经是你的好友", exception.getMessage());
    }

    @Test
    void removeFriend_Success() {
        // Arrange
        when(friendRelationMapper.delete(any(QueryWrapper.class))).thenReturn(1);

        // Act
        friendService.removeFriend(1L, 2L);

        // Assert
        verify(friendRelationMapper, times(2)).delete(any(QueryWrapper.class));
    }

    @Test
    void listFriends_Success() {
        // Arrange
        FriendRelation relation = new FriendRelation();
        relation.setId(10L);
        relation.setUserId(1L);
        relation.setFriendId(2L);
        relation.setStatus(FriendStatusEnum.NORMAL);
        relation.setCreateTime(LocalDateTime.now());

        when(friendRelationMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.singletonList(relation));
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(mockUser);

        // Act
        List<FriendResponse> responses = friendService.listFriends(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        
        FriendResponse resp = responses.get(0);
        assertEquals(10L, resp.getId());
        assertEquals(2L, resp.getFriendId());
        assertEquals(FriendStatusEnum.NORMAL, resp.getStatus());
        assertEquals("testUser", resp.getUsername());
        assertEquals("Test Nickname", resp.getNickname());
        
        verify(friendRelationMapper, times(1)).selectList(any(QueryWrapper.class));
        verify(userMapper, times(1)).selectOne(any(QueryWrapper.class));
    }
}