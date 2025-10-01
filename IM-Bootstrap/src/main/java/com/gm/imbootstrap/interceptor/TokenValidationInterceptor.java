package com.gm.imbootstrap.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.imbootstrap.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author: xexgm
 * @date: 2025/10/1
 * desc: Token验证拦截器
 */
@Slf4j
@Component
public class TokenValidationInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    // 不需要验证Token的接口路径
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/user/register",
        "/user/login",
        "/user/validate-token",
        "/error"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        log.debug("拦截器处理请求: {} {}", method, requestPath);

        // 跳过OPTIONS请求（CORS预检）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 检查是否是需要跳过验证的路径
        if (isExcludedPath(requestPath)) {
            log.debug("跳过Token验证: {}", requestPath);
            return true;
        }

        // 从请求头获取Token
        String token = extractTokenFromRequest(request);
        if (token == null || token.trim().isEmpty()) {
            log.warn("请求缺少Token: {}", requestPath);
            sendUnauthorizedResponse(response, "缺少访问凭证");
            return false;
        }

        // 验证Token
        if (!tokenService.validateToken(token)) {
            log.warn("Token验证失败: path={}, token={}", requestPath, token.substring(0, Math.min(token.length(), 20)) + "...");
            sendUnauthorizedResponse(response, "访问凭证无效或已过期");
            return false;
        }

        // 将用户ID添加到请求属性中，供后续使用
        Long userId = tokenService.getUserIdFromToken(token);
        if (userId != null) {
            request.setAttribute("currentUserId", userId);
            request.setAttribute("currentToken", token);
            log.debug("Token验证成功: userId={}, path={}", userId, requestPath);
        }

        return true;
    }

    /**
     * 检查路径是否需要跳过Token验证
     */
    private boolean isExcludedPath(String requestPath) {
        return EXCLUDED_PATHS.stream().anyMatch(requestPath::startsWith);
    }

    /**
     * 从请求中提取Token
     * 支持两种方式：
     * 1. Authorization头：Bearer token
     * 2. 请求参数：token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 方式1：从Authorization头获取
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 方式2：从请求参数获取
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.trim().isEmpty()) {
            return tokenParam;
        }

        // 方式3：从自定义头获取
        String tokenHeader = request.getHeader("X-Auth-Token");
        if (tokenHeader != null && !tokenHeader.trim().isEmpty()) {
            return tokenHeader;
        }

        return null;
    }

    /**
     * 发送未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(401, message, System.currentTimeMillis());
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * 错误响应DTO
     */
    public static class ErrorResponse {
        private int code;
        private String message;
        private long timestamp;

        public ErrorResponse(int code, String message, long timestamp) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public int getCode() { return code; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}