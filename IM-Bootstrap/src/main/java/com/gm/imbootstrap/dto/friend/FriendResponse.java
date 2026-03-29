package com.gm.imbootstrap.dto.friend;

import com.gm.graduation.common.domain.User;
import com.gm.graduation.common.enums.FriendStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendResponse {
    private Long id; // relation ID
    private Long friendId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private FriendStatusEnum status;
    private LocalDateTime createTime;
}
