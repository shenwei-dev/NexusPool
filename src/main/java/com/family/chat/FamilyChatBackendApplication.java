package com.family.chat;

import com.family.chat.netty.NettyServer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class FamilyChatBackendApplication implements CommandLineRunner {

    private final NettyServer nettyServer;

    /** Netty WebSocket 服务监听端口 */
    @Value("${netty.websocket.port:8081}")
    private int nettyPort;

    public static void main(String[] args) {
        SpringApplication.run(FamilyChatBackendApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // 启动 Netty WebSocket 服务
        nettyServer.start(nettyPort);
    }
}
