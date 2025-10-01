package com.gm.imbootstrap;

import com.gm.graduation.netty.server.NettyServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.concurrent.CompletableFuture;

@Slf4j
@SpringBootApplication
@MapperScan("com.gm.imbootstrap.mapper")
@ComponentScan({"com.gm.imbootstrap", "com.gm.graduation"})
public class ImBootstrapApplication {

    private NettyServer nettyServer;

    public static void main(String[] args) {
        SpringApplication.run(ImBootstrapApplication.class, args);
    }

    @PostConstruct
    public void startNettyServer() {
        nettyServer = new NettyServer();
        // 异步启动Netty服务器，避免阻塞SpringBoot启动
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始启动Netty服务器...");
                nettyServer.start();
            } catch (Exception e) {
                log.error("Netty服务器启动失败: {}", e.getMessage(), e);
            }
        });
        log.info("SpringBoot应用启动完成，Netty服务器正在异步启动中...");
    }

    @PreDestroy
    public void stopNettyServer() {
        if (nettyServer != null && nettyServer.isStarted()) {
            log.info("正在关闭Netty服务器...");
            nettyServer.shutdown();
            log.info("Netty服务器已关闭");
        }
    }
}
