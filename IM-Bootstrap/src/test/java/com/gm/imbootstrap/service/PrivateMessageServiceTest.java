package com.gm.imbootstrap.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gm.graduation.common.domain.PrivateMessage;
import com.gm.imbootstrap.mapper.PrivateMessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivateMessageServiceTest {

    @InjectMocks
    private PrivateMessageService privateMessageService;

    @Mock
    private PrivateMessageMapper privateMessageMapper;

    @Mock
    private MessageCryptoService messageCryptoService;

    @Test
    void saveMessage_ValidMessage_Success() {
        // Arrange
        PrivateMessage message = new PrivateMessage();
        message.setSenderId(1L);
        message.setReceiverId(2L);
        message.setContent("Hello");
        
        when(messageCryptoService.encrypt("Hello", "private:1:2")).thenReturn("encrypted-content");
        when(privateMessageMapper.insert(any(PrivateMessage.class))).thenReturn(1);

        // Act
        privateMessageService.saveMessage(message);

        // Assert
        verify(privateMessageMapper, times(1)).insert(message);
        assertEquals("encrypted-content", message.getContent());
    }

    @Test
    void saveMessage_NullMessage_DoNothing() {
        // Act
        privateMessageService.saveMessage(null);

        // Assert
        verify(privateMessageMapper, never()).insert(any(PrivateMessage.class));
    }

    @Test
    void getHistory_Success() {
        // Arrange
        Page<PrivateMessage> mockPage = new Page<>(1, 20);
        PrivateMessage message = new PrivateMessage();
        message.setMsgId(100L);
        message.setSenderId(1L);
        message.setReceiverId(2L);
        message.setContent("History msg");
        mockPage.setRecords(Collections.singletonList(message));
        mockPage.setTotal(1);

        when(messageCryptoService.decrypt("History msg", "private:1:2")).thenReturn("History msg");
        when(privateMessageMapper.selectPage(any(), any()))
                .thenReturn(mockPage);

        // Act
        Page<PrivateMessage> result = privateMessageService.getHistory(1L, 2L, 1, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("History msg", result.getRecords().get(0).getContent());
        
        verify(privateMessageMapper, times(1)).selectPage(any(), any());
    }
}