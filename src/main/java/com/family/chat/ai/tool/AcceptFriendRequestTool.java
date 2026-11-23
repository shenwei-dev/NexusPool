package com.family.chat.ai.tool;

import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiTool;
import com.family.chat.entity.FriendRequest;
import com.family.chat.enums.FriendRequestStatus;
import com.family.chat.mapper.FriendRequestMapper;
import com.family.chat.service.FriendRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工具: 接受好友申请 (写操作, 仅在用户明确表达意图时由模型调用)。
 */
@Component
@RequiredArgsConstructor
public class AcceptFriendRequestTool implements AiTool {

    private final FriendRequestMapper friendRequestMapper;

    private final FriendRequestService friendRequestService;

    @Override
    public String name() {
        return "accept_friend_request";
    }

    @Override
    public String description() {
        return "接受一条好友申请并自动建立好友关系。参数 requestId 来自 list_friend_requests 工具返回的申请ID。"
                + "仅当用户明确表示同意/接受某条好友申请时才调用";
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
        // Ownership guard: only the receiver of a pending request may accept it
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

        friendRequestService.acceptFriendRequest(requestId);
        result.put("success", true);
        result.put("newFriendId", request.getFromUserId());
        return result.toJSONString();
    }
}
