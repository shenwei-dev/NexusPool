package com.family.chat.ai.tool;

import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiTool;
import com.family.chat.mapper.FriendRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工具: 查询待处理好友申请数量。
 */
@Component
@RequiredArgsConstructor
public class QueryUnreadFriendRequestCountTool implements AiTool {

    private final FriendRequestMapper friendRequestMapper;

    @Override
    public String name() {
        return "query_unread_friend_request_count";
    }

    @Override
    public String description() {
        return "查询当前用户收到的、尚未处理(未同意也未拒绝)的好友申请总条数";
    }

    @Override
    public JSONObject parametersSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", new JSONObject());
        return schema;
    }

    @Override
    public String execute(Long userId, JSONObject args) {
        int count = friendRequestMapper.selectUnreadCount(userId);
        JSONObject result = new JSONObject();
        result.put("count", count);
        return result.toJSONString();
    }
}
