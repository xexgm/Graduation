package com.gm.imbootstrap.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gm.graduation.common.domain.ChatRoomMessage;
import com.gm.imbootstrap.mapper.ChatRoomMessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomMessageServiceTest {

    @InjectMocks
    private ChatRoomMessageService chatRoomMessageService;

    @Mock
    private ChatRoomMessageMapper chatRoomMessageMapper;

    @Mock
    private MessageCryptoService messageCryptoService;

    @Test
    void saveMessage_ValidMessage_EncryptBeforeInsert() {
        ChatRoomMessage message = new ChatRoomMessage();
        message.setRoomId(10L);
        message.setSenderId(1L);
        message.setContent("hello room");

        when(messageCryptoService.encrypt("hello room", "chatroom:10:1")).thenReturn("encrypted-room-content");
        when(chatRoomMessageMapper.insert(any(ChatRoomMessage.class))).thenReturn(1);

        chatRoomMessageService.saveMessage(message);

        verify(chatRoomMessageMapper, times(1)).insert(message);
        assertEquals("encrypted-room-content", message.getContent());
    }

    @Test
    void getHistory_Success_DecryptAfterQuery() {
        Page<ChatRoomMessage> mockPage = new Page<>(1, 20);
        ChatRoomMessage message = new ChatRoomMessage();
        message.setRoomId(10L);
        message.setSenderId(1L);
        message.setContent("encrypted-room-content");
        mockPage.setRecords(Collections.singletonList(message));
        mockPage.setTotal(1);

        when(chatRoomMessageMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(messageCryptoService.decrypt("encrypted-room-content", "chatroom:10:1")).thenReturn("hello room");

        Page<ChatRoomMessage> result = chatRoomMessageService.getHistory(10L, 1, 20);

        assertEquals(1, result.getTotal());
        assertEquals("hello room", result.getRecords().get(0).getContent());
        verify(chatRoomMessageMapper, times(1)).selectPage(any(), any());
    }
}
