package com.family.chat.ai.tool;

import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiTool;
import com.family.chat.constant.MessageType;
import com.family.chat.entity.Message;
import com.family.chat.entity.UserSessionProfile;
import com.family.chat.enums.MessageStatus;
import com.family.chat.mapper.ChatSessionMapper;
import com.family.chat.mapper.FriendMapper;
import com.family.chat.mapper.MessageMapper;
import com.family.chat.mapper.UserMapper;
import com.family.chat.netty.SessionManager;
import com.family.chat.protobuf.MessageProto;
import com.family.chat.util.UserIdentifierGenerator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 工具: 代当前用户给好友发送文本消息 (写操作)。
 *
 * <p>完整复刻正常消息链路: 好友关系校验 -> 落库 -> 序列号 -> 双向会话更新
 * -> MQ 投递(离线兜底) -> 在线直推。任何一环缺失都会造成消息丢失或不一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendMessageTool implements AiTool {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final MessageMapper messageMapper;

    private final ChatSessionMapper chatSessionMapper;

    private final FriendMapper friendMapper;

    private final UserMapper userMapper;

    private final SessionManager sessionManager;

    private final RabbitTemplate rabbitTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String name() {
        return "send_message_on_behalf";
    }

    @Override
    public String description() {
        return "代当前用户给指定好友发送一条文本消息, 对方会像收到普通聊天消息一样实时收到。"
                + "仅当用户明确说'帮我给某人发消息'时才调用, 消息内容必须忠实于用户原话, 不得自行发挥";
    }

    @Override
    public JSONObject parametersSchema() {
        JSONObject friendId = new JSONObject();
        friendId.put("type", "integer");
        friendId.put("description", "接收方好友的用户ID");

        JSONObject content = new JSONObject();
        content.put("type", "string");
        content.put("description", "消息文本内容, 不超过500字");

        JSONObject properties = new JSONObject();
        properties.put("friendId", friendId);
        properties.put("content", content);

        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[]{"friendId", "content"});
        return schema;
    }

    @Override
    public String execute(Long userId, JSONObject args) {
        Long friendId = args.getLong("friendId");
        String content = args.getString("content");
        JSONObject result = new JSONObject();

        if (friendId == null || content == null || content.isBlank()) {
            result.put("success", false);
            result.put("reason", "参数不合法, 需要 friendId 与非空 content");
            return result.toJSONString();
        }
        if (friendId.equals(userId)) {
            result.put("success", false);
            result.put("reason", "不能给自己发消息");
            return result.toJSONString();
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            result.put("success", false);
            result.put("reason", "消息内容超过" + MAX_CONTENT_LENGTH + "字上限");
            return result.toJSONString();
        }
        // Permission guard: only friends may receive agent-sent messages
        if (friendMapper.exists(userId, friendId) <= 0) {
            result.put("success", false);
            result.put("reason", "对方不是当前用户的好友, 无法发送");
            return result.toJSONString();
        }

        long now = System.currentTimeMillis();
        Message message = new Message();
        message.setId(UserIdentifierGenerator.nextId());
        message.setFromUserId(userId);
        message.setToUserId(friendId);
        message.setMessageType(MessageType.TEXT);
        message.setContent(content);
        message.setStatus(MessageStatus.UNREAD.getCode());
        message.setSequence(nextSequence(userId, friendId));
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        messageMapper.insert(message);

        chatSessionMapper.incrementUnreadCount(friendId, userId, content, now);
        chatSessionMapper.updateSenderSession(userId, friendId, content, now);

        try {
            rabbitTemplate.convertAndSend("chat.exchange", "chat", message);
        } catch (Exception e) {
            log.error("Agent message failed to enqueue to RabbitMQ. [messageId={}]", message.getId(), e);
        }

        Channel receiverChannel = sessionManager.getChannel(friendId);
        if (receiverChannel != null && receiverChannel.isActive()) {
            UserSessionProfile senderProfile = userMapper.selectSessionProfile(userId, friendId);
            String displayName = senderProfile != null ? senderProfile.getDisplayName() : "新消息";
            MessageProto.MsgFrame pushData = MessageProto.MsgFrame.newBuilder()
                    .setMessageId(message.getId())
                    .setSenderId(userId)
                    .setReceiverId(friendId)
                    .setContent(content)
                    .setTimestamp(now)
                    .setType(MessageType.TEXT)
                    .setSequence(message.getSequence())
                    .setSenderName(displayName)
                    .build();
            receiverChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(pushData.toByteArray())));
        }

        log.info("Agent sent message on behalf of user. [userId={}, friendId={}, messageId={}]",
                userId, friendId, message.getId());
        result.put("success", true);
        result.put("messageId", message.getId());
        return result.toJSONString();
    }

    /** 与正常消息链路一致的会话序列号: Redis 原子自增, 键按双方 ID 归一化 */
    private Long nextSequence(Long userA, Long userB) {
        long minId = Math.min(userA, userB);
        long maxId = Math.max(userA, userB);
        return stringRedisTemplate.opsForValue().increment("chat:seq:" + minId + "_" + maxId);
    }
}
