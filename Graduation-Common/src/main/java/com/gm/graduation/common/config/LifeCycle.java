package com.gm.graduation.common.config;

/**
 * @author: xexgm
 * @date: 2025/9/29
 */
public interface LifeCycle {

    /**
     * 初始化
     */
    void init();

    /**
     * 启动
     */
    void start();

    /**
     * 关闭
     */
    void shutdown();

    /**
     * 是否启动
     * @return
     */
    boolean isStarted();
}
