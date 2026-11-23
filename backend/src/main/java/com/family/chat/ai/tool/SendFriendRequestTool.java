package com.family.chat.ai.tool;

import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiTool;
import com.family.chat.entity.FriendRequest;
import com.family.chat.entity.User;
import com.family.chat.mapper.FriendMapper;
import com.family.chat.mapper.FriendRequestMapper;
import com.family.chat.mapper.UserMapper;
import com.family.chat.netty.SessionManager;
import com.family.chat.protobuf.MessageProto;
import com.family.chat.util.UserIdentifierGenerator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工具: 代当前用户向指定账号发送好友申请 (写操作)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendFriendRequestTool implements AiTool {

    private final UserMapper userMapper;

    private final FriendMapper friendMapper;

    private final FriendRequestMapper friendRequestMapper;

    private final SessionManager sessionManager;

    @Override
    public String name() {
        return "send_friend_request";
    }

    @Override
    public String description() {
        return "代当前用户向指定账号发送好友申请。可先用 search_user_by_account 确认账号存在。"
                + "仅当用户明确要求添加某人为好友时才调用";
    }

    @Override
    public JSONObject parametersSchema() {
        JSONObject account = new JSONObject();
        account.put("type", "integer");
        account.put("description", "目标用户的账号(数字)");

        JSONObject greeting = new JSONObject();
        greeting.put("type", "string");
        greeting.put("description", "验证消息/打招呼内容, 可选");

        JSONObject properties = new JSONObject();
        properties.put("account", account);
        properties.put("greeting", greeting);

        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[]{"account"});
        return schema;
    }

    @Override
    public String execute(Long userId, JSONObject args) {
        Long account = args.getLong("account");
        String greeting = args.getString("greeting");
        JSONObject result = new JSONObject();
        if (account == null) {
            result.put("success", false);
            result.put("reason", "缺少参数 account");
            return result.toJSONString();
        }

        User target = userMapper.selectByAccount(account);
        if (target == null || (target.getDeleted() != null && target.getDeleted())) {
            result.put("success", false);
            result.put("reason", "未找到该账号对应的用户");
            return result.toJSONString();
        }
        if (target.getId().equals(userId)) {
            result.put("success", false);
            result.put("reason", "不能添加自己为好友");
            return result.toJSONString();
        }
        if (friendMapper.exists(userId, target.getId()) > 0) {
            result.put("success", false);
            result.put("reason", "对方已经是好友, 无需重复申请");
            return result.toJSONString();
        }
        if (friendRequestMapper.existsPendingRequest(userId, target.getId())) {
            result.put("success", false);
            result.put("reason", "双方之间已有待处理的好友申请");
            return result.toJSONString();
        }

        long now = System.currentTimeMillis();
        FriendRequest request = new FriendRequest();
        request.setId(UserIdentifierGenerator.nextId());
        request.setFromUserId(userId);
        request.setToUserId(target.getId());
        request.setGreeting(greeting);
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        friendRequestMapper.insert(request);

        // Realtime badge signal, consistent with the HTTP friend-request flow
        Channel receiverChannel = sessionManager.getChannel(target.getId());
        if (receiverChannel != null && receiverChannel.isActive()) {
            MessageProto.MsgFrame pushProto = MessageProto.MsgFrame.newBuilder()
                    .setSenderId(userId)
                    .setContent(greeting == null ? "" : greeting)
                    .setType(6)
                    .setSequence(1L)
                    .build();
            receiverChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(pushProto.toByteArray())));
        }

        log.info("Agent sent friend request on behalf of user. [userId={}, targetAccount={}]", userId, account);
        result.put("success", true);
        result.put("targetNickname", target.getNickname());
        return result.toJSONString();
    }
}
