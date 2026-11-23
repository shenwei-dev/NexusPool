package com.family.chat.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultThreadFactory;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NettyServer {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    /** SSL 上下文，供同包下的 WebSocketChannelInitializer 为每条通道创建加密处理器 */
    static SslContext sslContext;

    // 主线程组，用于接收客户端连接（线程命名便于线程栈分析与问题定位）
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("netty-boss"));

    // 工作线程组，用于处理网络读写逻辑
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("netty-worker"));

    private ChannelFuture channelFuture;

    /** 停机保护标志，防止重复关闭 */
    private final AtomicBoolean shutdownGuard = new AtomicBoolean(false);

    /**
     * 启动 Netty WebSocket 服务
     * @param port 监听的端口号 (例如 8081)
     */
    public void start(int port) {
        // 启动线程必须显式命名，禁止匿名线程（便于线上问题排查）
        Thread starterThread = new Thread(() -> {
            try {
                // ==================== 【WSS 核心：自签名证书构建】 ====================
                // Netty 自带的证书工具，在本地内存自动生成并签发一套 SSL/TLS 证书
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
                log.info("WSS encrypted channel ready, self-signed SSL/TLS certificate loaded.");
                // ===================================================================

                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new WebSocketChannelInitializer()); // 绑定管道处理器

                channelFuture = bootstrap.bind(port).sync();
                log.info("Netty WebSocket server started. [port={}]", port);

                // 等待服务端监听端口关闭
                channelFuture.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("Failed to start Netty server.", e);
            } finally {
                destroy();
            }
        }, "netty-server-starter");
        starterThread.start();
    }

    @PreDestroy
    public void destroy() {
        // 保证关闭逻辑只执行一次
        if (!shutdownGuard.compareAndSet(false, true)) {
            return;
        }
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        log.info("Netty WebSocket server shut down gracefully.");
    }
}
