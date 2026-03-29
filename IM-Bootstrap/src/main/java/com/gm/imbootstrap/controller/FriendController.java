package com.gm.imbootstrap.controller;

import com.gm.imbootstrap.dto.ApiResponse;
import com.gm.imbootstrap.dto.friend.AddFriendRequest;
import com.gm.imbootstrap.dto.friend.FriendResponse;
import com.gm.imbootstrap.service.FriendService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: xexgm
 * desc: 用户好友管理接口
 */
@Slf4j
@RestController
@RequestMapping("/friend")
public class FriendController {

    @Autowired
    private FriendService friendService;

    /** 添加好友 */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<String>> addFriend(@RequestBody AddFriendRequest req, HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getAttribute("currentUserId");
            if (currentUserId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或凭证无效"));
            }
            if (req.getFriendId() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("目标用户ID不能为空"));
            }

            friendService.addFriend(currentUserId, req.getFriendId());
            return ResponseEntity.ok(ApiResponse.success("添加好友成功", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("添加好友失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("添加好友失败"));
        }
    }

    /** 删除好友 */
    @DeleteMapping("/remove/{friendId}")
    public ResponseEntity<ApiResponse<String>> removeFriend(@PathVariable Long friendId, HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getAttribute("currentUserId");
            if (currentUserId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或凭证无效"));
            }

            friendService.removeFriend(currentUserId, friendId);
            return ResponseEntity.ok(ApiResponse.success("删除好友成功", null));
        } catch (Exception e) {
            log.error("删除好友失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("删除好友失败"));
        }
    }

    /** 获取好友列表 */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> listFriends(HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getAttribute("currentUserId");
            if (currentUserId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或凭证无效"));
            }

            List<FriendResponse> friends = friendService.listFriends(currentUserId);
            return ResponseEntity.ok(ApiResponse.success("获取成功", friends));
        } catch (Exception e) {
            log.error("获取好友列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取好友列表失败"));
        }
    }
}
