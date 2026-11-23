package com.family.chat.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiAgentService;
import com.family.chat.constant.MessageType;
import com.family.chat.constant.WsOperationCode;
import com.family.chat.dto.MessageDTO;
import com.family.chat.entity.ChatSession;
import com.family.chat.entity.Message;
import com.family.chat.entity.UserSessionProfile;
import com.family.chat.enums.MessageStatus;
import com.family.chat.mapper.ChatSessionMapper;
import com.family.chat.mapper.MessageMapper;
import com.family.chat.mapper.UserMapper;
import com.family.chat.metrics.ChatMetrics;
import com.family.chat.netty.SessionManager;
import com.family.chat.protobuf.MessageProto;
import com.family.chat.service.MessageService;
import com.family.chat.service.OssService;
import com.family.chat.util.UserIdentifierGenerator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 消息服务实现。
 *
 * <p>负责处理 WebSocket 入站消息与音视频信令，完成幂等校验、消息落库、ACK 回执、会话更新以及消息推送/路由。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    /** Redis（字符串）操作模板：用于幂等键与序列号生成 */
    private final StringRedisTemplate stringRedisTemplate;

    /** RabbitMQ 消息模板：用于异步投递消息到消费者 */
    private final RabbitTemplate rabbitTemplate;

    /** 消息持久化 Mapper */
    private final MessageMapper messageMapper;

    /** 在线会话管理器：用于定位接收方 Channel 并推送 */
    private final SessionManager sessionManager;

    /** 会话列表 Mapper：用于更新会话未读数与最后一条消息 */
    private final ChatSessionMapper chatSessionMapper;

    /** Redisson 客户端：用于分布式锁等能力 */
    private final RedissonClient redissonClient;

    /** 用户数据访问：用于查询会话展示信息 */
    private final UserMapper userMapper;

    /** 对象存储服务：用于头像/图片签名与校验 */
    private final OssService ossService;

    /** 指标埋点：用于统计消息处理耗时与分布 */
    private final ChatMetrics chatMetrics;

    /** AI 智能体服务：用于处理 AI 对话与工具调用链 */
    private final AiAgentService aiAgentService;

    /** AI 处理专用线程池：避免阻塞 Netty IO 线程 */
    @Autowired
    @Qualifier("aiExecutor")
    private Executor aiExecutor;

    /**
     * 处理 WebSocket 入站业务消息。
     *
     * @param messageRequest 客户端消息体
     * @param clientChannel  客户端连接通道
     */
    @Override
    public void handleWebSocketMessage(MessageDTO messageRequest, Channel clientChannel) {
        int optionCode = messageRequest.getType() != null ? messageRequest.getType() : 0;
        Long realUserId = messageRequest.getSenderId();
        switch (optionCode) {
            case WsOperationCode.TEXT_MESSAGE:
                // 处理普通消息
                processNormalTextMessage(messageRequest, clientChannel);
                break;

            case WsOperationCode.VOICE_CALL_INVITE:
                // 发起语音通话呼叫
                processVoiceCallInvite(messageRequest, clientChannel);
                break;

            case WsOperationCode.VOICE_CALL_ACCEPT:
                // 接受语音通话
                processVoiceCallAccept(messageRequest, clientChannel);
                break;

            case WsOperationCode.VOICE_CALL_HANGUP:
                // 挂断语音通话
                processVoiceCallHangup(messageRequest, clientChannel);
                break;

            case WsOperationCode.VOICE_CALL_REFUSE:
                // 拒绝语音通话邀请
                processVoiceCallRefuse(messageRequest, clientChannel);
                break;

            case WsOperationCode.VOICE_CALL_CANCEL:
                // 取消语音通话呼叫
                processVoiceCallCancel(messageRequest, clientChannel);
                break;

            case WsOperationCode.VIDEO_CALL_INVITE:
                // 发起视频通话呼叫
                processVideoCallInvite(messageRequest, clientChannel);
                break;

            case WsOperationCode.VIDEO_CALL_ACCEPT:
                // 接受视频通话
                processVideoCallAccept(messageRequest, clientChannel);
                break;

            case WsOperationCode.VIDEO_CALL_HANGUP:
                // 挂断视频通话
                processVideoCallHangup(messageRequest, clientChannel);
                break;

            case WsOperationCode.VIDEO_CALL_REFUSE:
                // 拒绝视频通话邀请
                processVideoCallRefuse(messageRequest, clientChannel);
                break;

            case WsOperationCode.VIDEO_CALL_CANCEL:
                // 取消视频通话呼叫
                processVideoCallCancel(messageRequest, clientChannel);
                break;

            case WsOperationCode.LOCATION_MESSAGE:
                // 发送位置消息
                processLocationMessage(messageRequest, clientChannel);
                break;

            case WsOperationCode.IMAGE_MESSAGE:
                // 发送图片消息
                processImageMessage(messageRequest, clientChannel);
                break;

            case WsOperationCode.AI_MESSAGE:
                // 向AI发送消息
                processAiAgentMessage(messageRequest, clientChannel);
                break;

            case WsOperationCode.HEARTBEAT:
                // Heartbeat is a high-frequency low-value event, keep it at DEBUG to avoid log flooding
                log.debug("Received heartbeat ping. [userId={}]", realUserId);
                break;

            default:
                log.warn("Received undefined ws operation code. [operationCode={}]", optionCode);
                clientChannel.writeAndFlush(new TextWebSocketFrame("{\"status\":\"error\",\"message\":\"未知操作码\"}"));
                break;
        }
    }

    /**
     * 拉取会话增量消息（基于本地最大序列号）。
     *
     * @param currentUserId 当前用户 ID
     * @param friendId      对端用户 ID
     * @param localMaxSeq   客户端已拥有的最大序列号
     * @return 增量消息列表
     */
    @Override
    public List<Message> getDeltaMessages(Long currentUserId, Long friendId, Long localMaxSeq) {
        return messageMapper.selectDeltaMessages(currentUserId, friendId, localMaxSeq);
    }

    /**
     * 更新发送方会话信息。
     *
     * <p>采用“先更新、更新失败再加锁插入”的策略，降低锁竞争；加锁后再次双重检查更新，避免并发插入冲突。
     *
     * @param userId    会话所属用户 ID
     * @param friendId  对端用户 ID
     * @param content   最新一条消息摘要
     * @param timestamp 更新时间戳（毫秒）
     */
    private void updateSenderChatSession(Long userId, Long friendId, String content, Long timestamp) {
        // 步骤一：首先无锁尝试 UPDATE 现有行（读写分离，大部分情况下此更新会成功，无需走锁开销）
        int affectedRows = chatSessionMapper.updateSenderSession(userId, friendId, content, timestamp);
        if (affectedRows == 0) {
            long minId = Math.min(userId, friendId);
            long maxId = Math.max(userId, friendId);
            String lockKey = "lock:session:" + minId + "_" + maxId; //
            RLock lock = redissonClient.getLock(lockKey);
            try {
                // 尝试获取分布式锁，最多等待 5 秒，获取成功后 10 秒自动释放（防止发生线程死锁挂死）
                if (lock.tryLock(5, 10, java.util.concurrent.TimeUnit.SECONDS)) {

                    // 双重检查：等待锁期间其他线程可能已完成插入
                    int doubleCheckRows = chatSessionMapper.updateSenderSession(userId, friendId, content, timestamp);

                    if (doubleCheckRows == 0) {
                        // 确认还是没有，此时当前线程可以安全执行 INSERT，绝对不会报唯一键冲突或死锁！
                        ChatSession session = new ChatSession();
                        session.setId(UserIdentifierGenerator.nextId());
                        session.setUserId(userId);
                        session.setFriendId(friendId);
                        session.setLastMessage(content);
                        session.setUnreadCount(0);
                        session.setUpdatedAt(timestamp);

                        chatSessionMapper.insert(session);
                        log.info("Chat session created via double-checked locking. [lockKey={}]", lockKey);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while acquiring distributed session lock.", e);
            } finally {
                // 仅在当前线程持锁时释放，避免误解锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * 获取会话维度的递增序列号。
     *
     * <p>使用 Redis INCR 生成单调递增序列，用于消息排序与增量拉取。
     *
     * @param userA 用户 A
     * @param userB 用户 B
     * @return 会话内递增序列号
     */
    private Long getNextSequence(Long userA, Long userB) {
        long minId = Math.min(userA, userB);
        long maxId = Math.max(userA, userB);
        String redisKey = "chat:seq:" + minId + "_" + maxId;

        return stringRedisTemplate.opsForValue().increment(redisKey);
    }

    /**
     * 处理 AI 智能体消息。
     *
     * <p>先落库并下发 ACK，再异步执行智能体编排并推送最终回复，避免阻塞 Netty IO 线程。
     *
     * @param messageRequest 客户端请求
     * @param clientChannel  客户端通道
     */
    private void processAiAgentMessage(MessageDTO messageRequest, Channel clientChannel) {
        Message userMessage = new Message();
        userMessage.setId(UserIdentifierGenerator.nextId());
        userMessage.setFromUserId(messageRequest.getSenderId());
        userMessage.setToUserId(AiAgentService.AI_ASSISTANT_ID);
        userMessage.setMessageType(messageRequest.getType());
        userMessage.setContent(messageRequest.getContent());
        userMessage.setStatus(MessageStatus.UNREAD.getCode());
        userMessage.setCreatedAt(System.currentTimeMillis());
        userMessage.setUpdatedAt(System.currentTimeMillis());
        Long seq = getNextSequence(userMessage.getFromUserId(), userMessage.getToUserId());
        userMessage.setSequence(seq);

        try {
            messageMapper.insert(userMessage);
            updateSenderChatSession(messageRequest.getSenderId(), AiAgentService.AI_ASSISTANT_ID, messageRequest.getContent(), userMessage.getCreatedAt());
        } catch (Exception e) {
            log.error("Failed to persist user question message.", e);
            JSONObject failedAck = new JSONObject();
            failedAck.put("status", "failed");
            failedAck.put("tempId", messageRequest.getTempId());
            clientChannel.writeAndFlush(new TextWebSocketFrame(failedAck.toJSONString()));
            return;
        }
        MessageProto.MsgFrame successAckProto = MessageProto.MsgFrame.newBuilder()
                .setStatus("success")
                .setTempId(messageRequest.getTempId())
                .setMessageId(userMessage.getId())
                .setSequence(userMessage.getSequence())
                .build();
        byte[] successAckBytes = successAckProto.toByteArray();
        clientChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(successAckBytes)));

        // Dispatch the ReAct agent loop to the dedicated thread pool, never block the Netty IO thread
        aiExecutor.execute(() -> {
            try {
                log.info("Agent chain started. [userId={}, promptLength={}]", messageRequest.getSenderId(), messageRequest.getContent().length());

                // Run the ReAct orchestration (tool calling loop) and get the final assistant reply
                String agentReply = aiAgentService.chat(messageRequest.getSenderId(), messageRequest.getContent());

                log.info("Agent chain finished. [userId={}, replyLength={}]", messageRequest.getSenderId(), agentReply.length());
                Long seq1 = getNextSequence(userMessage.getFromUserId(), userMessage.getToUserId());

                // Persist the final assistant reply so it participates in history and unread counting
                Message aiMessage = new Message();
                aiMessage.setId(UserIdentifierGenerator.nextId());
                aiMessage.setFromUserId(AiAgentService.AI_ASSISTANT_ID);
                aiMessage.setToUserId(messageRequest.getSenderId());
                aiMessage.setMessageType(0);
                aiMessage.setContent(agentReply);
                aiMessage.setStatus(MessageStatus.UNREAD.getCode());
                aiMessage.setSequence(seq1);
                aiMessage.setCreatedAt(System.currentTimeMillis());
                aiMessage.setUpdatedAt(System.currentTimeMillis());

                messageMapper.insert(aiMessage);
                updateSenderChatSession(messageRequest.getSenderId(), AiAgentService.AI_ASSISTANT_ID, agentReply, aiMessage.getCreatedAt());
                MessageProto.MsgFrame pushDataProto = MessageProto.MsgFrame.newBuilder()
                        .setMessageId(aiMessage.getId())
                        .setSenderId(AiAgentService.AI_ASSISTANT_ID)
                        .setReceiverId(messageRequest.getSenderId())
                        .setContent(agentReply)
                        .setTimestamp(aiMessage.getCreatedAt())
                        .setType(MessageType.TEXT)
                        .setSequence(seq1)
                        .build();
                byte[] pushDataBytes = pushDataProto.toByteArray();
                clientChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(pushDataBytes)));

            } catch (Exception e) {
                log.error("Exception occurred in agent processing. [userId={}]", messageRequest.getSenderId(), e);
            }
        });
    }

    /**
     * 处理器一：处理普通聊天信息 (Type = 0)
     */
    private void processNormalTextMessage(MessageDTO messageRequest, Channel clientChannel) {
        chatMetrics.recordMessageProcess("text",
                () -> processChatMessage(messageRequest, clientChannel, MessageType.TEXT, MessageType.TEXT));
    }

    /**
     * 聊天消息通用处理流程：幂等校验 -> 消息落库 -> ACK 回执 -> 会话更新 -> MQ 投递 -> 在线直推。
     * 文本、图片、位置消息仅消息类型编码不同，流程完全一致。
     *
     * @param dbMessageType   落库使用的消息类型编码
     * @param pushMessageType 下行推送携带的消息类型编码
     */
    private void processChatMessage(MessageDTO messageRequest, Channel clientChannel, int dbMessageType, int pushMessageType) {
        String redisKey = "msg:idempotent:" + messageRequest.getTempId();
        Boolean isUnique = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", 300, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isUnique)) {
            log.warn("Duplicate message delivery detected, replying success ack directly. [tempId={}]", messageRequest.getTempId());
            MessageProto.MsgFrame ackProto = MessageProto.MsgFrame.newBuilder()
                    .setStatus("success")
                    .setTempId(messageRequest.getTempId())
                    .build();
            byte[] ackBytes = ackProto.toByteArray();
            clientChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(ackBytes)));
            return;
        }

        // 将用户发送过来的消息封装成Message对象
        Message message = new Message();
        message.setId(UserIdentifierGenerator.nextId());
        message.setFromUserId(messageRequest.getSenderId());
        message.setToUserId(messageRequest.getReceiverId());
        message.setMessageType(dbMessageType);
        message.setContent(messageRequest.getContent());
        message.setStatus(MessageStatus.UNREAD.getCode());
        message.setCreatedAt(System.currentTimeMillis());
        message.setUpdatedAt(System.currentTimeMillis());
        Long seq = getNextSequence(message.getFromUserId(), message.getToUserId());
        message.setSequence(seq);

        // 用户消息落库
        try {
            messageMapper.insert(message);
        } catch (Exception e) {
            log.error("Failed to persist message.", e);
            MessageProto.MsgFrame ackProto = MessageProto.MsgFrame.newBuilder()
                    .setStatus("false")
                    .setTempId(messageRequest.getTempId())
                    .build();
            byte[] failedAckBytes = ackProto.toByteArray();
            clientChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(failedAckBytes)));
            return;
        }

        // 发送ACK包确认消息发送成功
        MessageProto.MsgFrame successAckProto = MessageProto.MsgFrame.newBuilder()
                .setStatus("success")
                .setTempId(messageRequest.getTempId())
                .setMessageId(message.getId())
                .setSequence(message.getSequence())
                .build();
        log.info("sequence: {}", message.getSequence());
        byte[] ackBytes = successAckProto.toByteArray();
        clientChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(ackBytes)));

        // 更新会话未读数
        chatSessionMapper.incrementUnreadCount(
                messageRequest.getReceiverId(),
                messageRequest.getSenderId(),
                messageRequest.getContent(),
                System.currentTimeMillis()
        );

        updateSenderChatSession(message.getFromUserId(), message.getToUserId(), message.getContent(), message.getCreatedAt());

        try {
            // 将消息发送到RabbitMQ
            rabbitTemplate.convertAndSend(
                    "chat.exchange",
                    "chat",
                    message
            );
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ.", e);
        }

        Channel receiverChannel = sessionManager.getChannel(message.getToUserId());
        boolean isOnline = receiverChannel != null && receiverChannel.isActive();

        updateReceiverChatSession(message.getToUserId(), message.getFromUserId(), message.getContent(), message.getCreatedAt(), isOnline);

        if (isOnline) {
            log.info("Receiver online on current node, pushing via local channel.");
            updateSenderChatSession(message.getToUserId(), message.getFromUserId(), message.getContent(), message.getCreatedAt());

            UserSessionProfile senderProfile = userMapper.selectSessionProfile(message.getFromUserId(), message.getToUserId());
            String displayName = senderProfile != null ? senderProfile.getDisplayName() : "新消息";
            String avatarUrl = senderProfile != null ? ossService.resolveAvatarUrl(senderProfile.getAvatarUrl()) : "";

            // Image content persists an object key; sign it into an accessible URL before pushing
            String pushContent = pushMessageType == MessageType.IMAGE
                    ? ossService.resolveStoredUrl(message.getContent())
                    : message.getContent();
            MessageProto.MsgFrame pushData = MessageProto.MsgFrame.newBuilder()
                    .setMessageId(message.getId())
                    .setSenderId(message.getFromUserId())
                    .setContent(pushContent)
                    .setTimestamp(message.getCreatedAt())
                    .setType(pushMessageType)
                    .setSequence(message.getSequence())
                    .setReceiverId(message.getToUserId())
                    .setSenderName(displayName)
                    .setSenderAvatarUrl(avatarUrl)
                    .build();
            byte[] pushDataBytes = pushData.toByteArray();
            log.debug("Pushing message to receiver. [senderName={}]", displayName);
            receiverChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(pushDataBytes)));
        } else {
            log.info("Receiver not found on current node, will broadcast route message.");
        }
    }

    /**
     * 发送成功 ACK。
     *
     * @param tempId        客户端临时消息 ID
     * @param clientChannel 客户端通道
     */
    private void sendSuccessAck(Long tempId, Channel clientChannel) {
        MessageProto.MsgFrame successAckProto = MessageProto.MsgFrame.newBuilder()
                .setStatus("success")
                .setTempId(tempId)
                .build();
        byte[] ackBytes = successAckProto.toByteArray();
        clientChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(ackBytes)));
    }
    
    /**
     * 将 RTC 信令转发给接收方（仅在线直推）。
     *
     * @param messageRequest 信令消息
     */
    private void routeSignalingToReceiver(MessageDTO messageRequest) {
        Channel receiverChannel = sessionManager.getChannel(messageRequest.getReceiverId());
    
        if (receiverChannel != null && receiverChannel.isActive()) {
            MessageProto.MsgFrame pushData = MessageProto.MsgFrame.newBuilder()
                    .setMessageId(UserIdentifierGenerator.nextId())
                    .setTempId(messageRequest.getTempId())
                    .setContent(messageRequest.getContent())
                    .setTimestamp(System.currentTimeMillis())
                    .setType(messageRequest.getType())
                    .build();
            byte[] pushDataBytes = pushData.toByteArray();
            receiverChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(pushDataBytes)));
        } else {
            log.warn("Receiver offline, RTC signal cannot be relayed. [receiverId={}]", messageRequest.getReceiverId());
        }
    }

    /**
     * 更新接收方会话信息（必要时初始化会话记录）。
     *
     * @param userId    会话所属用户 ID
     * @param friendId  对端用户 ID
     * @param content   最新一条消息摘要
     * @param timestamp 更新时间戳（毫秒）
     * @param isOnline  接收方是否在线（在线不累加未读）
     */
    private void updateReceiverChatSession(Long userId, Long friendId, String content, Long timestamp, boolean isOnline) {
        // 1. 尝试执行更新。如果在线则不累加未读数，如果离线则累加未读数
        int affectedRows;
        if (isOnline) {
            affectedRows = chatSessionMapper.updateSenderSession(userId, friendId, content, timestamp);
        } else {
            affectedRows = chatSessionMapper.incrementUnreadCount(userId, friendId, content, timestamp);
        }

        // 2. 如果影响行数为 0，说明接收方数据库中还没有创建过该会话，立即执行自动初始化插入！
        if (affectedRows == 0) {
            log.info("Receiver session not found, creating automatically. [userId={}, friendId={}]", userId, friendId);

            ChatSession session = new ChatSession();
            session.setId(UserIdentifierGenerator.nextId());
            session.setUserId(userId);
            session.setFriendId(friendId);
            session.setLastMessage(content);

            // 如果在线设为 0，离线设为 1
            session.setUnreadCount(isOnline ? 0 : 1);
            session.setUpdatedAt(timestamp);

            try {
                chatSessionMapper.insert(session);
                log.info("Receiver session initialized successfully. [userId={}]", userId);
            } catch (Exception e) {
                // 并发防护：如果报唯一键冲突，重新更新一次即可
                if (isOnline) {
                    chatSessionMapper.updateSenderSession(userId, friendId, content, timestamp);
                } else {
                    chatSessionMapper.incrementUnreadCount(userId, friendId, content, timestamp);
                }
            }
        }
    }

    /**
     * 处理位置消息。
     *
     * @param messageRequest 客户端消息
     * @param clientChannel  客户端通道
     */
    private void processLocationMessage(MessageDTO messageRequest, Channel clientChannel) {
        chatMetrics.recordMessageProcess("location",
                () -> processChatMessage(messageRequest, clientChannel, MessageType.LOCATION, MessageType.LOCATION));
    }

    /**
     * 处理图片消息。
     *
     * <p>仅允许签发过的对象 Key，防止伪造 key 触发服务端签名任意对象。
     *
     * @param messageRequest 客户端消息
     * @param clientChannel  客户端通道
     */
    private void processImageMessage(MessageDTO messageRequest, Channel clientChannel) {
        String content = messageRequest.getContent();
        // 图片内容必须是本服务签发台账内的对象 key, 防止伪造任意 key 诱导服务端签名
        if (content == null || !content.startsWith("chat/images/") || !ossService.isKeyIssued(content)) {
            log.warn("Reject image message with untrusted object key. [userId={}]", messageRequest.getSenderId());
            MessageProto.MsgFrame ackProto = MessageProto.MsgFrame.newBuilder()
                    .setStatus("false")
                    .setTempId(messageRequest.getTempId())
                    .build();
            clientChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(ackProto.toByteArray())));
            return;
        }
        chatMetrics.recordMessageProcess("image",
                () -> processChatMessage(messageRequest, clientChannel, MessageType.IMAGE, MessageType.IMAGE));
    }

    /**
     * 1. 发起语音通话呼叫邀请 (type = 1)
     */
    private void processVoiceCallInvite(MessageDTO messageRequest, Channel clientChannel) {
        log.info("Voice call invite. [senderId={}, receiverId={}, room={}]",
                messageRequest.getSenderId(), messageRequest.getReceiverId(), messageRequest.getContent());
        relayRtcInviteSignal(messageRequest, clientChannel, "voice", 4);
    }

    /**
     * 2. 接受语音通话 (type = 2)
     */
    private void processVoiceCallAccept(MessageDTO messageRequest, Channel clientChannel) {
        log.info("Voice call accepted. [senderId={}, receiverId={}]",
                messageRequest.getSenderId(), messageRequest.getReceiverId());
        relayRtcDirectSignal(messageRequest, "voice_accept");
    }

    /**
     * 3. 挂断语音通话 (type = 3)
     */
    private void processVoiceCallHangup(MessageDTO messageRequest, Channel clientChannel) {
        log.info("Voice call hung up. [senderId={}, receiverId={}]",
                messageRequest.getSenderId(), messageRequest.getReceiverId());

        relayRtcDirectSignal(messageRequest, "voice_hangup");
    }

    /**
     * 4. 拒绝语音通话邀请 (type = 4)
     */
    private void processVoiceCallRefuse(MessageDTO messageRequest, Channel clientChannel) {
        log.info("Voice call refused. [senderId={}, receiverId={}]",
                messageRequest.getSenderId(), messageRequest.getReceiverId());
        relayRtcDirectSignal(messageRequest, "voice_refuse");
    }

    /**
     * 5. 取消语音通话呼叫 (type = 5)
     */
    private void processVoiceCallCancel(MessageDTO messageRequest, Channel clientChannel) {
        log.info("Voice call cancelled. [senderId={}, receiverId={}]",
                messageRequest.getSenderId(), messageRequest.getReceiverId());
        relayRtcDirectSignal(messageRequest, "voice_cancel");
    }

    /**
     * 6. 发起视频通话呼叫邀请 (type = 6)
     */
    private void processVideoCallInvite(MessageDTO messageRequest, Channel clientChannel) {
        log.info("Video call invite. [senderId={}, receiverId={}, room={}]",
                messageRequest.getSenderId(), messageRequest.getReceiverId(), messageRequest.getContent());

        relayRtcInviteSignal(messageRequest, clientChannel, "video", 9);
    }

    /**
     * 7. 接受视频通话 (type = 7)
     */
    private void processVideoCallAccept(MessageDTO messageRequest, Channel clientChannel) {
        log.info("Video call accepted. [senderId={}, receiverId={}]",
                messageRequest.getSenderId(), messageRequest.getReceiverId());

        relayRtcDirectSignal(messageRequest, "video_accept");
    }

    /**
     * 8. 挂断视频通话 (type = 8)
     */
    private void processVideoCallHangup(MessageDTO messageRequest, Channel clientChannel) {
        log.info("Video call hung up. [senderId={}, receiverId={}]",
                messageRequest.getSenderId(), messageRequest.getReceiverId());

        relayRtcDirectSignal(messageRequest, "video_hangup");
    }

    /**
     * 9. 拒绝视频通话邀请 (type = 9)
     */
    private void processVideoCallRefuse(MessageDTO messageRequest, Channel clientChannel) {
        log.info("Video call refused. [senderId={}, receiverId={}]",
                messageRequest.getSenderId(), messageRequest.getReceiverId());

        relayRtcDirectSignal(messageRequest, "video_refuse");
    }

    /**
     * 10. 取消视频通话呼叫 (type = 10)
     */
    private void processVideoCallCancel(MessageDTO messageRequest, Channel clientChannel) {
        log.info("Video call cancelled. [senderId={}, receiverId={}]",
                messageRequest.getSenderId(), messageRequest.getReceiverId());

        relayRtcDirectSignal(messageRequest, "video_cancel");
    }

    /**
     * 中转【呼叫邀请】信令 (带在线检测 + 离线自动回传占位/拒绝)
     *
     * @param refuseCode 离线时回传给主叫方的拒绝 code (语音传 4，视频传 9)
     */
    private void relayRtcInviteSignal(MessageDTO messageRequest, Channel senderChannel, String callTypeStr, int refuseCode) {
        // 获取接受方ID
        Long receiverId = messageRequest.getReceiverId();

        // 在通道管理器中查找被呼叫方的NettyChannel
        Channel targetChannel = sessionManager.getChannel(receiverId);

        if (targetChannel != null && targetChannel.isActive()) {
            // 将JSON对象封装成字符串
            String jsonPayload = com.alibaba.fastjson2.JSON.toJSONString(messageRequest);

            // 推送数据
            targetChannel.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(jsonPayload));
            log.info("Call invite relayed to online receiver. [callType={}, receiverId={}]", callTypeStr, receiverId);
        } else {
            // Case B: receiver offline, reply busy/unreachable signal to caller immediately to avoid infinite waiting
            log.warn("Receiver offline or busy, replying unreachable signal to caller. [receiverId={}, callerId={}, callType={}]", receiverId, messageRequest.getSenderId(), callTypeStr);

            MessageDTO busyResponse = new MessageDTO();
            busyResponse.setSenderId(receiverId);
            busyResponse.setReceiverId(messageRequest.getSenderId());
            busyResponse.setType(refuseCode); // 4 代表语音拒绝/占线，9 代表视频拒绝/占线
            busyResponse.setContent("对方离线/占线中");

            senderChannel.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(
                    com.alibaba.fastjson2.JSON.toJSONString(busyResponse)));
        }
    }


    /**
     * 中转【控制信令】 (接听 / 拒绝 / 取消 / 挂断：拿到目标 Channel 直接发送)
     */
    private void relayRtcDirectSignal(MessageDTO messageRequest, String actionName) {
        Long receiverId = messageRequest.getReceiverId();
        Channel targetChannel = sessionManager.getChannel(receiverId);

        if (targetChannel != null && targetChannel.isActive()) {
            MessageProto.MsgFrame pushData = MessageProto.MsgFrame.newBuilder()
                    .setSenderId(messageRequest.getSenderId())
                    .setReceiverId(messageRequest.getReceiverId())
                    .setContent(messageRequest.getContent())
                    .setTimestamp(System.currentTimeMillis())
                    .setType(messageRequest.getType())
                    .build();
            byte[] pushDataBytes = pushData.toByteArray();
            targetChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(pushDataBytes)));
            log.info("RTC control signal relayed. [action={}, receiverId={}]", actionName, receiverId);
        } else {
            log.warn("Failed to relay RTC control signal, receiver offline or disconnected. [action={}, receiverId={}]", actionName, receiverId);
        }
    }
}
