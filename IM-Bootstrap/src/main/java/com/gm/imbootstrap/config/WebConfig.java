package com.gm.imbootstrap.config;

import com.gm.imbootstrap.interceptor.TokenValidationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author: xexgm
 * @date: 2025/10/1
 * desc: Web配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TokenValidationInterceptor tokenValidationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenValidationInterceptor)
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns(
                    "/user/register",    // 注册接口
                    "/user/login",       // 登录接口
                    "/user/validate-token", // Token验证接口
                    "/error",            // 错误页面
                    "/favicon.ico",      // 图标
                    "/swagger-ui/**",    // Swagger文档（如果有）
                    "/v3/api-docs/**"    // API文档（如果有）
                );
    }
}