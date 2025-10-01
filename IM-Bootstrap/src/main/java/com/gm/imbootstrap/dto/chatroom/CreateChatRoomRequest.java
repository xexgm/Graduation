package com.gm.imbootstrap.dto.chatroom;

import com.gm.graduation.common.enums.ChatRoomTypeEnum;
import lombok.Data;

@Data
public class CreateChatRoomRequest {
    private String roomName;
    private String description;
    private ChatRoomTypeEnum roomType; // 传值示例：PUBLIC_ROOM / PRIVATE_ROOM
}