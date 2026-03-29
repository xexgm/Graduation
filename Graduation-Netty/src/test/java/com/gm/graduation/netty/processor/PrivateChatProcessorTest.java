package com.gm.graduation.netty.processor;

import com.gm.graduation.common.api.IPrivateMessageService;
import com.gm.graduation.common.domain.CompleteMessage;
import com.gm.graduation.common.domain.PrivateMessage;
import com.gm.graduation.netty.cache.UserLinkManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrivateChatProcessorTest {

    private IPrivateMessageService mockMessageService;
    private ChannelHandlerContext mockCtx;
    private Channel mockChannel;

    @BeforeEach
    void setUp() {
        mockMessageService = Mockito.mock(IPrivateMessageService.class);
        PrivateChatProcessor.setPrivateMessageService(mockMessageService);

        mockCtx = Mockito.mock(ChannelHandlerContext.class);
        mockChannel = Mockito.mock(Channel.class);
        when(mockCtx.channel()).thenReturn(mockChannel);
    }

    @AfterEach
    void tearDown() {
        UserLinkManager.removeUserChannel(2L);
        PrivateChatProcessor.setPrivateMessageService(null);
    }

    @Test
    void process_ReceiverOffline_SaveAsUnread() {
        // Arrange
        CompleteMessage msg = new CompleteMessage();
        msg.setUid(1L);
        msg.setToId(2L);
        msg.setContent("Hello Offline");

        // Act
        PrivateChatProcessor.getInstance().process(mockCtx, msg);

        // Assert
        ArgumentCaptor<PrivateMessage> captor = ArgumentCaptor.forClass(PrivateMessage.class);
        verify(mockMessageService, times(1)).saveMessage(captor.capture());

        PrivateMessage savedMessage = captor.getValue();
        assertEquals(1L, savedMessage.getSenderId());
        assertEquals(2L, savedMessage.getReceiverId());
        assertEquals("Hello Offline", savedMessage.getContent());
        assertEquals(0, savedMessage.getIsRead()); // 离线未读
    }

    @Test
    void process_ReceiverOnline_SaveAsReadAndPush() {
        // Arrange
        CompleteMessage msg = new CompleteMessage();
        msg.setUid(1L);
        msg.setToId(2L);
        msg.setContent("Hello Online");
        msg.setAppId(2);
        msg.setMessageType(1);

        // 模拟接收者在线
        when(mockChannel.isActive()).thenReturn(true);
        UserLinkManager.addUserChannel(2L, mockCtx);

        // Act
        PrivateChatProcessor.getInstance().process(Mockito.mock(ChannelHandlerContext.class), msg);

        // Assert
        // 1. 验证落库
        ArgumentCaptor<PrivateMessage> captor = ArgumentCaptor.forClass(PrivateMessage.class);
        verify(mockMessageService, times(1)).saveMessage(captor.capture());

        PrivateMessage savedMessage = captor.getValue();
        assertEquals(1L, savedMessage.getSenderId());
        assertEquals(2L, savedMessage.getReceiverId());
        assertEquals("Hello Online", savedMessage.getContent());
        assertEquals(1, savedMessage.getIsRead()); // 在线已读

        // 2. 验证实时推送
        ArgumentCaptor<CompleteMessage> pushCaptor = ArgumentCaptor.forClass(CompleteMessage.class);
        verify(mockCtx, times(1)).writeAndFlush(pushCaptor.capture());

        CompleteMessage pushedMsg = pushCaptor.getValue();
        assertEquals(2, pushedMsg.getAppId());
        assertEquals(1L, pushedMsg.getUid());
        assertEquals(2L, pushedMsg.getToId());
        assertEquals("Hello Online", pushedMsg.getContent());
        assertNotNull(pushedMsg.getTimeStamp());
    }

    @Test
    void process_IncompleteMessage_DoNothing() {
        // Arrange
        CompleteMessage msg = new CompleteMessage();
        msg.setUid(1L);
        // 没有设置 toId 和 content

        // Act
        PrivateChatProcessor.getInstance().process(mockCtx, msg);

        // Assert
        verify(mockMessageService, never()).saveMessage(any());
        verify(mockCtx, never()).writeAndFlush(any());
    }
}