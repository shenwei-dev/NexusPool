package com.family.chat.netty;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private final ConcurrentHashMap<Long, Channel> onlineUsers = new ConcurrentHashMap<>();

    public void bind(Long userId, Channel channel) {
        onlineUsers.put(userId, channel);
    }

    /**
     * 解绑通道。
     * 使用条件移除 remove(key, value)，防止用户重连后旧通道下线时误删新会话。
     */
    public void unbind(Channel channel) {
        Long userId = channel.attr(HandshakeAuthHandler.USER_ID_KEY).get();
        if (userId != null) {
            onlineUsers.remove(userId, channel);
        } else {
            onlineUsers.values().remove(channel);
        }
    }

    public Channel getChannel(Long userId) {
        return onlineUsers.get(userId);
    }

    public int getOnlineCount() {
        return onlineUsers.size();
    }
}
