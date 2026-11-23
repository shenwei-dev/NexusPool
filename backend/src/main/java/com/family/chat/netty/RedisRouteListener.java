package com.family.chat.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class RedisRouteListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisRouteListener.class);

    @Autowired
    private SessionManager sessionManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String jsonText = new String(message.getBody());
            JSONObject pushData = JSON.parseObject(jsonText);
            Long receiverId = pushData.getLong("receiverId");

            // 👈 【分布式路由判定】：检查接收者 B 的物理连接是否在“我当前这一台”Netty 实例上
            Channel receiverChannel = sessionManager.getChannel(receiverId);

            if (receiverChannel != null && receiverChannel.isActive()) {
                log.info("Route broadcast hit current node, pushing to local channel. [receiverId={}]", receiverId);
                // 执行真正的直推
                receiverChannel.writeAndFlush(new TextWebSocketFrame(jsonText));
            }
        } catch (Exception e) {
            log.error("Failed to parse route broadcast message.", e);
        }
    }
}