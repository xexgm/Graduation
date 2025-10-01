package com.gm.imbootstrap.controller;

import java.util.List;
import com.gm.graduation.common.domain.ChatRoom;
import com.gm.imbootstrap.dto.ApiResponse;
import com.gm.imbootstrap.dto.chatroom.CreateChatRoomRequest;
import com.gm.imbootstrap.service.ChatRoomService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聊天室Controller
 */
@Slf4j
@RestController
@RequestMapping("/chatroom")
public class ChatRoomController {

    @Autowired
    private ChatRoomService chatRoomService;

    /** 创建一个聊天室 */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ChatRoom>> create(@RequestBody CreateChatRoomRequest req,
                                                        HttpServletRequest request) {
        try {
            Long operatorId = (Long) request.getAttribute("currentUserId");
            if (operatorId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或凭证无效"));
            }

            ChatRoom room = chatRoomService.create(
                operatorId,
                req.getRoomName(),
                req.getDescription(),
                req.getRoomType()
            );
            return ResponseEntity.ok(ApiResponse.success("创建成功", room));
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("创建聊天室失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("创建失败: " + e.getMessage()));
        }
    }

    /** 下线聊天室 */
    @PostMapping("/{roomId}/offline")
    public ResponseEntity<ApiResponse<String>> offline(@PathVariable Long roomId,
                                                       HttpServletRequest request) {
        try {
            Long operatorId = (Long) request.getAttribute("currentUserId");
            if (operatorId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或凭证无效"));
            }

            chatRoomService.offline(operatorId, roomId);
            return ResponseEntity.ok(ApiResponse.success("下线成功", null));
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("下线聊天室失败: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("下线失败: " + e.getMessage()));
        }
    }

    /** 删除聊天室（软删，更新为删除状态） */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long roomId,
                                                      HttpServletRequest request) {
        try {
            Long operatorId = (Long) request.getAttribute("currentUserId");
            if (operatorId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或凭证无效"));
            }

            chatRoomService.delete(operatorId, roomId);
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("删除聊天室失败: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }

    /** 查询所有未删除聊天室 */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<ChatRoom>>> listAll() {
        try {
            List<ChatRoom> rooms = chatRoomService.listAllNotDeleted();
            return ResponseEntity.ok(ApiResponse.success("查询成功", rooms));
        } catch (Exception e) {
            log.error("查询聊天室失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("查询失败"));
        }
    }
}
