package com.gm.imbootstrap.controller;

import com.gm.graduation.common.domain.User;
import com.gm.imbootstrap.dto.ApiResponse;
import com.gm.imbootstrap.dto.ChangePasswordRequest;
import com.gm.imbootstrap.dto.LoginRequest;
import com.gm.imbootstrap.dto.LoginResponse;
import com.gm.imbootstrap.dto.TokenRequest;
import com.gm.imbootstrap.service.UserService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: xexgm
 * @date: 2025/9/29
 * des: 用户接口
 */
@Slf4j
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
    public ResponseEntity<ApiResponse<User>> register(@RequestBody User user) {
        try {
            User registeredUser = userService.register(user);
            log.info("用户注册接口调用成功: username={}", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("注册成功", registeredUser));
        } catch (IllegalArgumentException e) {
            log.warn("用户注册参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }



    /**
     * 用户登录接口
     * @param loginRequest 包含用户名和密码
     * @return 登录结果，包含用户信息和Token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        try {
            UserService.LoginResult loginResult = userService.login(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );

            LoginResponse response = new LoginResponse(loginResult.getUser(), loginResult.getToken());
            log.info("用户登录接口调用成功: username={}", loginRequest.getUsername());
            return ResponseEntity.ok(ApiResponse.success("登录成功", response));
        } catch (IllegalArgumentException e) {
            log.warn("用户登录参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("用户登录失败: username={}, error={}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 用于接收登出请求的DTO
     */
    @Data
    public static class LogoutRequest {
        private Long userId;
        private String token; // 可选，用于额外验证
    }

    /**
     * 用户登出接口
     * @param logoutRequest 包含要登出的用户ID
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody LogoutRequest logoutRequest) {
        try {
            if (logoutRequest == null || logoutRequest.getUserId() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("需要提供用户ID"));
            }

            userService.logout(logoutRequest.getUserId());
            log.info("用户登出接口调用成功: userId={}", logoutRequest.getUserId());
            return ResponseEntity.ok(ApiResponse.success("登出成功", null));
        } catch (Exception e) {
            log.error("用户登出失败: userId={}, error={}",
                logoutRequest != null ? logoutRequest.getUserId() : null, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取用户信息接口
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<User>> getUserInfo(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success("获取成功", user));
        } catch (Exception e) {
            log.error("获取用户信息失败: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("获取用户信息失败"));
        }
    }

    /**
     * 验证Token接口
     * @param tokenRequest Token验证请求
     * @return 验证结果和用户信息
     */
    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse<User>> validateToken(@RequestBody TokenRequest tokenRequest) {
        try {
            if (tokenRequest == null || tokenRequest.getToken() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Token不能为空"));
            }

            User user = userService.validateTokenAndGetUser(tokenRequest.getToken());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Token无效或已过期"));
            }

            return ResponseEntity.ok(ApiResponse.success("Token验证成功", user));
        } catch (Exception e) {
            log.error("Token验证失败: error={}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Token验证失败"));
        }
    }

    /**
     * 修改密码接口
     * @param changePasswordRequest 修改密码请求
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        try {
            if (changePasswordRequest == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("请求参数不能为空"));
            }

            userService.changePassword(
                changePasswordRequest.getUserId(),
                changePasswordRequest.getOldPassword(),
                changePasswordRequest.getNewPassword()
            );

            log.info("用户修改密码接口调用成功: userId={}", changePasswordRequest.getUserId());
            return ResponseEntity.ok(ApiResponse.success("密码修改成功，请重新登录", null));
        } catch (IllegalArgumentException e) {
            log.warn("修改密码参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("修改密码失败: userId={}, error={}",
                changePasswordRequest != null ? changePasswordRequest.getUserId() : null, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

}
