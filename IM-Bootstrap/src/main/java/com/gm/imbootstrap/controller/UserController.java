package com.gm.imbootstrap.controller;

import com.gm.graduation.common.domain.User;
import com.gm.imbootstrap.service.UserService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author: xexgm
 * @date: 2025/9/29
 * des: 用户接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册接口
     * @param user 包含用户名和密码
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = userService.register(user);
            // 为了安全，不应在响应中返回密码
            registeredUser.setPassword(null);
            return ResponseEntity.ok(registeredUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 用于接收登录请求的DTO
     */
    @Data
    private static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 用户登录接口
     * @param loginRequest 包含用户名和密码
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            User user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
            // 为了安全，不应在响应中返回密码
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 用户登出接口
     * @param user 包含要登出的用户ID
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody User user) {
        if (user == null || user.getUserId() == null) {
            return ResponseEntity.badRequest().body("需要提供用户ID");
        }
        userService.logout(user.getUserId());
        return ResponseEntity.ok("登出成功");
    }
}
