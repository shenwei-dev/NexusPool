package com.family.chat.netty;

import com.family.chat.service.MessageService;
import com.family.chat.util.JwtUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter; // 改用这个更稳定的基类
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HandshakeAuthHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(HandshakeAuthHandler.class);

    public static final AttributeKey<Long> USER_ID_KEY = AttributeKey.valueOf("userId");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            String uri = request.uri();
            if (uri.startsWith("/ws")) {
                QueryStringDecoder decoder = new QueryStringDecoder(uri);
                List<String> tokens = decoder.parameters().get("token");
                if (tokens != null && !tokens.isEmpty()) {
                    String token = tokens.get(0);
                    try {
                        JwtUtil jwtUtil = SpringUtil.getBean(JwtUtil.class);
                        Long realUserId = jwtUtil.getUserId(token);
                        ctx.channel().attr(USER_ID_KEY).set(realUserId);
                        SessionManager sessionManager = SpringUtil.getBean(SessionManager.class);
                        sessionManager.bind(realUserId, ctx.channel());
                        log.info("Handshake auth passed, user bound. [userId={}, onlineCount={}]",
                                realUserId, sessionManager.getOnlineCount());
                    } catch (Exception e) {
                        log.error("Token verification failed, rejecting connection.", e);
                        ctx.close();
                        return;
                    }
                } else {
                    log.error("Token missing in handshake request, rejecting connection.");
                    ctx.close();
                    return;
                }
                request.setUri("/ws");
            }
        }
        ctx.fireChannelRead(msg);
    }
}