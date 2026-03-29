package com.gm.imbootstrap.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gm.graduation.common.domain.ChatRoomMessage;
import com.gm.graduation.common.domain.PrivateMessage;
import com.gm.imbootstrap.dto.ApiResponse;
import com.gm.imbootstrap.service.ChatRoomMessageService;
import com.gm.imbootstrap.service.PrivateMessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author: xexgm
 * desc: 历史消息接口
 */
@Slf4j
@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private PrivateMessageService privateMessageService;

    @Autowired
    private ChatRoomMessageService chatRoomMessageService;

    /** 获取私聊历史消息 */
    @GetMapping("/private/history")
    public ResponseEntity<ApiResponse<Page<PrivateMessage>>> getPrivateHistory(
            @RequestParam Long friendId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getAttribute("currentUserId");
            if (currentUserId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或凭证无效"));
            }

            Page<PrivateMessage> history = privateMessageService.getHistory(currentUserId, friendId, current, size);
            return ResponseEntity.ok(ApiResponse.success("获取成功", history));
        } catch (Exception e) {
            log.error("获取私聊历史记录失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取失败"));
        }
    }

    /** 获取聊天室历史消息 */
    @GetMapping("/chatroom/history")
    public ResponseEntity<ApiResponse<Page<ChatRoomMessage>>> getChatRoomHistory(
            @RequestParam Long roomId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getAttribute("currentUserId");
            if (currentUserId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或凭证无效"));
            }

            Page<ChatRoomMessage> history = chatRoomMessageService.getHistory(roomId, current, size);
            return ResponseEntity.ok(ApiResponse.success("获取成功", history));
        } catch (Exception e) {
            log.error("获取聊天室历史记录失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取失败"));
        }
    }
}
