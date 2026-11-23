package com.family.chat.netty;

import com.family.chat.dto.MessageDTO;
import com.family.chat.protobuf.MessageProto;
import com.family.chat.service.FriendRequestService;
import com.family.chat.service.MessageService;
import com.family.chat.util.UserIdentifierGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * WebSocket 消息入站处理器。
 *
 * <p>负责解析客户端发送的二进制 Protobuf 帧，并将业务消息交由服务层处理；同时处理握手完成、心跳超时等事件，
 * 维护用户与 Channel 的在线绑定关系。
 */
public class ChatHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    /** WebSocket 处理链路日志 */
    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);

    /**
     * 读取并处理客户端入站帧。
     *
     * <p>当前仅处理二进制帧（Protobuf），并将 traceId/userId 写入 MDC，避免 Netty IO 线程复用导致日志上下文串写。
     *
     * @param ctx   Channel 上下文
     * @param frame WebSocket 帧
     * @throws Exception 解析或处理过程中出现异常
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof BinaryWebSocketFrame) {
            // Netty IO 线程会被复用, MDC 必须遵循"谁放入谁清理"原则, 防止日志上下文串写
            MDC.put("traceId", String.valueOf(UserIdentifierGenerator.nextId()));
            try {
                ByteBuf content = frame.content();
                byte[] bytes = new byte[content.readableBytes()];
                content.readBytes(bytes);
                MessageProto.MsgFrame protoMsg = MessageProto.MsgFrame.parseFrom(bytes);
                // High-frequency event, keep it at DEBUG to avoid log flooding
                log.debug("Protobuf frame parsed. [type={}]", protoMsg.getType());
                MessageDTO dto = new MessageDTO();
                dto.setTempId(protoMsg.getTempId());
                dto.setReceiverId(protoMsg.getReceiverId());
                dto.setContent(protoMsg.getContent());
                dto.setType(protoMsg.getType());
                Long realUserId = ctx.channel().attr(HandshakeAuthHandler.USER_ID_KEY).get();
                if (realUserId == null) {
                    log.error("Channel not bound to a valid userId, closing channel.");
                    ctx.close();
                    return;
                }
                // Inject the user dimension into the log context so Grafana can filter per-user WS traces
                MDC.put("userId", String.valueOf(realUserId));
                dto.setSenderId(realUserId);
                MessageService messageService = SpringUtil.getBean(MessageService.class);
                messageService.handleWebSocketMessage(dto, ctx.channel());
            } finally {
                MDC.remove("traceId");
                MDC.remove("userId");
            }
        }
    }

    /**
     * 处理 Channel 从 pipeline 移除事件。
     *
     * <p>用于清理用户在线绑定，并记录当前在线人数。
     *
     * @param ctx Channel 上下文
     * @throws Exception 处理过程中出现异常
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        SessionManager sessionManager = SpringUtil.getBean(SessionManager.class);
        sessionManager.unbind(ctx.channel());
        log.info("Client offline, channel unbound. [channelId={}, remainingOnline={}]",
                ctx.channel().id().asShortText(), sessionManager.getOnlineCount());
    }

    /**
     * 处理 Channel 异常事件。
     *
     * @param ctx   Channel 上下文
     * @param cause 异常原因
     * @throws Exception 处理过程中出现异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Channel disconnected unexpectedly.", cause);
        ctx.close();
    }

    /**
     * 处理用户事件（心跳超时、握手完成等）。
     *
     * @param ctx Channel 上下文
     * @param evt 事件对象
     * @throws Exception 处理过程中出现异常
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.READER_IDLE) {
                log.warn("Heartbeat timeout detected, closing idle channel. [channelId={}]", ctx.channel().id().asShortText());
                ctx.close();
                return;
            }
        }
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            Long realUserId = ctx.channel().attr(HandshakeAuthHandler.USER_ID_KEY).get();
            if (realUserId != null) {
                SessionManager sessionManager = SpringUtil.getBean(SessionManager.class);
                sessionManager.bind(realUserId, ctx.channel());
                log.info("WebSocket handshake completed, user online. [userId={}]", realUserId);

                FriendRequestService friendRequestService = SpringUtil.getBean(FriendRequestService.class);
                friendRequestService.pushOfflineFriendRequests(realUserId, ctx.channel());
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}
