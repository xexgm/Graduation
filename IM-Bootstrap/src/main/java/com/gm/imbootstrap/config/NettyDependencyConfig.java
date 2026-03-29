package com.gm.imbootstrap.config;

import com.gm.graduation.common.api.IPrivateMessageService;
import com.gm.graduation.netty.processor.PrivateChatProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class NettyDependencyConfig {

    @Autowired
    private IPrivateMessageService privateMessageService;

    @PostConstruct
    public void init() {
        PrivateChatProcessor.setPrivateMessageService(privateMessageService);
    }
}
