package com.family.chat.ai.tool;

import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiTool;
import com.family.chat.entity.FriendRequest;
import com.family.chat.enums.FriendRequestStatus;
import com.family.chat.mapper.FriendRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工具: 拒绝好友申请 (写操作, 仅在用户明确表达意图时由模型调用)。
 */
@Component
@RequiredArgsConstructor
public class RejectFriendRequestTool implements AiTool {

    private final FriendRequestMapper friendRequestMapper;

    @Override
    public String name() {
        return "reject_friend_request";
    }

    @Override
    public String description() {
        return "拒绝一条好友申请。参数 requestId 来自 list_friend_requests 工具返回的申请ID。"
                + "仅当用户明确表示拒绝某条好友申请时才调用";
    }

    @Override
    public JSONObject parametersSchema() {
        JSONObject requestId = new JSONObject();
        requestId.put("type", "integer");
        requestId.put("description", "好友申请ID");

        JSONObject properties = new JSONObject();
        properties.put("requestId", requestId);

        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[]{"requestId"});
        return schema;
    }

    @Override
    public String execute(Long userId, JSONObject args) {
        Long requestId = args.getLong("requestId");
        JSONObject result = new JSONObject();
        if (requestId == null) {
            result.put("success", false);
            result.put("reason", "缺少参数 requestId");
            return result.toJSONString();
        }

        FriendRequest request = friendRequestMapper.selectById(requestId);
        // Ownership guard: only the receiver of a pending request may reject it
        if (request == null || !userId.equals(request.getToUserId())) {
            result.put("success", false);
            result.put("reason", "好友申请不存在或不属于当前用户");
            return result.toJSONString();
        }
        if (request.getStatus() != null && request.getStatus() != FriendRequestStatus.PENDING.getCode()) {
            result.put("success", false);
            result.put("reason", "该申请已被处理过");
            return result.toJSONString();
        }

        friendRequestMapper.updateStatus(requestId, FriendRequestStatus.REJECTED.getCode(), System.currentTimeMillis());
        result.put("success", true);
        return result.toJSONString();
    }
}
