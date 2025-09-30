package com.gm.graduation.netty.server;

import java.util.concurrent.atomic.AtomicBoolean;

import com.gm.graduation.common.config.LifeCycle;
import com.gm.graduation.common.utils.NettyConfig;
import com.gm.graduation.common.utils.SystemUtil;
import com.gm.graduation.netty.handler.BusinessHandler;
import com.gm.graduation.netty.handler.MessageToWebSocketFrameEncoder;
import com.gm.graduation.netty.handler.WebSocketFrameToMessageDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import static com.gm.graduation.common.constant.LinkConfigConstant.LISTENING_PORT;

/**
 * @author: xexgm
 * @date: 2025/9/29
 * des: Netty服务端
 */
@Slf4j
public class NettyServer implements LifeCycle {

    private final AtomicBoolean started = new AtomicBoolean(false);

    private ServerBootstrap serverBootstrap;
    private EventLoopGroup bossEventLoopGroup;
    private EventLoopGroup workerEventLoopGroup;

    public NettyServer() {
    }

    /**
     * 初始化事件循环组
     */
    @Override
    public void init() {
        serverBootstrap = new ServerBootstrap();
        if (SystemUtil.useEpollMode()) {
            bossEventLoopGroup = new EpollEventLoopGroup(NettyConfig.bossEventLoopGroupNum,
                new DefaultThreadFactory("epoll-netty-boss-nio"));
            workerEventLoopGroup = new EpollEventLoopGroup(NettyConfig.workerEventLoopGroupNum,
                new DefaultThreadFactory("epoll-netty-worker-nio"));
        } else {
            bossEventLoopGroup = new NioEventLoopGroup(NettyConfig.bossEventLoopGroupNum,
                new DefaultThreadFactory("default-netty-boss-nio"));
            workerEventLoopGroup = new NioEventLoopGroup(NettyConfig.workerEventLoopGroupNum,
                new DefaultThreadFactory("default-netty-worker-nio"));
        }
        log.info("[InitEventLoopGroup] bossEventLoopGroup: {}, workerEventLoopGroup: {}",
            bossEventLoopGroup.getClass().getName(), workerEventLoopGroup.getClass().getName());
    }

    @Override
    public void start() {
        init();
        serverBootstrap
            .group(bossEventLoopGroup, workerEventLoopGroup)
            // 设置服务端 通信类型（基于TCP）
            .channel(SystemUtil.useEpollMode() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
            // 设置ChannelPipeline，也就是业务职责链，由处理的handler串联，从线程池处理
            .childHandler(new ChannelInitializer<SocketChannel>() {
                // 添加处理的handler，通常包括 消息编解码、业务处理、也可以有日志、权限、过滤
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                        // http 编解码器
                        .addLast(new HttpServerCodec())
                        // http消息的多个部分聚合为一个完整的 http 请求
                        .addLast(new HttpObjectAggregator(65536))
                        // websocket 协议处理器
                        .addLast(new WebSocketServerProtocolHandler("/ws"))
                        // 解码器
                        .addLast(new WebSocketFrameToMessageDecoder())
                        // 编码器
                        .addLast(new MessageToWebSocketFrameEncoder())
                        // 自定义业务处理器
                        .addLast(new BusinessHandler());
                }
            })
            // bootstrap 还可以设置tcp参数，根据需要可以分别设置主线程池和从线程池参数，来优化性能
            // 主线程池使用 option方法来设置，从线程池使用 childOption方法设置
            // boss线程只负责接收新连接，当TCP三次握手完成后：
            // 若boss线程处理速度不够快，未及时处理的连接会堆积在操作系统的全连接队列中
            // 该队列长度由SO_BACKLOG参数控制
            // 客户端请求 -> 操作系统SYN队列 -> 完成握手后进入ACCEPT队列（长度由SO_BACKLOG控制）
            //           -> boss线程逐个取出ACCEPT队列中的连接进行后续处理
            .option(ChannelOption.SO_BACKLOG, 1024)
            // 表示连接保活，相当于心跳机制，默认7200s，TCP协议栈实现，os内核自动发送心跳包
            .childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            // 绑定端口，启动 select线程，轮询监听channel时间，监听到事件后，交给从线程池处理
            ChannelFuture future = serverBootstrap.bind(LISTENING_PORT).sync();
            // 启动标志
            this.started.compareAndSet(false, true);
            log.info("[NettyServer started] 初始化完成，监听端口: {}", LISTENING_PORT);
            // 等待服务端口关闭
            future.channel().closeFuture().addListener(f -> {
                this.shutdown();
                log.info("NettyServer 关闭");
            });
        } catch (InterruptedException e) {
            log.info("server 初始化异常: {}", e.getMessage());
        }
    }

    /**
     * 关闭
     */
    @Override
    public void shutdown() {
        // 未启动
        if (!this.started.get()) return;
        // 关闭
        if (bossEventLoopGroup != null) {
            bossEventLoopGroup.shutdownGracefully();
        }
        if (workerEventLoopGroup != null) {
            workerEventLoopGroup.shutdownGracefully();
        }
        this.started.compareAndSet(true, false);
    }

    @Override
    public boolean isStarted() {
        return this.started.get();
    }
}
