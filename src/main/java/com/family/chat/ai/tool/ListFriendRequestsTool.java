package com.family.chat.ai.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiTool;
import com.family.chat.entity.FriendRequest;
import com.family.chat.enums.FriendRequestStatus;
import com.family.chat.mapper.FriendRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具: 查询收到的好友申请列表(含申请 ID, 供接受申请等后续工具使用)。
 */
@Component
@RequiredArgsConstructor
public class ListFriendRequestsTool implements AiTool {

    private final FriendRequestMapper friendRequestMapper;

    @Override
    public String name() {
        return "list_friend_requests";
    }

    @Override
    public String description() {
        return "查询当前用户收到的好友申请列表, 返回申请ID(requestId)、申请人ID、申请人昵称、验证消息与处理状态。"
                + "用户询问有哪些好友申请、谁想加他时使用";
    }

    @Override
    public JSONObject parametersSchema() {
        JSONObject properties = new JSONObject();

        JSONObject onlyPending = new JSONObject();
        onlyPending.put("type", "boolean");
        onlyPending.put("description", "是否只返回待处理(未同意未拒绝)的申请, 默认 true");
        properties.put("onlyPending", onlyPending);

        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    @Override
    public String execute(Long userId, JSONObject args) {
        boolean onlyPending = args.getBooleanValue("onlyPending") || !args.containsKey("onlyPending");

        List<FriendRequest> incoming = friendRequestMapper.selectIncoming(userId);
        JSONArray requests = new JSONArray();
        for (FriendRequest request : incoming) {
            if (request.getDeleted() != null && request.getDeleted() == 1) {
                continue;
            }
            if (onlyPending && (request.getStatus() == null
                    || request.getStatus() != FriendRequestStatus.PENDING.getCode())) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("requestId", request.getId());
            item.put("fromUserId", request.getFromUserId());
            item.put("greeting", request.getGreeting());
            item.put("status", request.getStatus());
            requests.add(item);
        }

        JSONObject result = new JSONObject();
        result.put("total", requests.size());
        result.put("requests", requests);
        return result.toJSONString();
    }
}
