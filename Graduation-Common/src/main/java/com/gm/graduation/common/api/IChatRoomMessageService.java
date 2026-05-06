package com.gm.graduation.common.api;

import com.gm.graduation.common.domain.ChatRoomMessage;

public interface IChatRoomMessageService {

    void saveMessage(ChatRoomMessage message);
}
