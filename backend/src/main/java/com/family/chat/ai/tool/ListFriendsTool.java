package com.family.chat.ai.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiTool;
import com.family.chat.mapper.FriendMapper;
import com.family.chat.vo.QueryFriendListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具: 查询当前用户的好友列表。
 */
@Component
@RequiredArgsConstructor
public class ListFriendsTool implements AiTool {

    private final FriendMapper friendMapper;

    @Override
    public String name() {
        return "list_friends";
    }

    @Override
    public String description() {
        return "查询当前用户的好友列表, 返回好友ID(friendId)、昵称与备注。"
                + "用户询问有哪些好友、想给某位好友发消息时先用它拿到 friendId";
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
        List<QueryFriendListVO> friends = friendMapper.selectFriendList(userId);
        JSONArray items = new JSONArray();
        for (QueryFriendListVO friend : friends) {
            JSONObject item = new JSONObject();
            item.put("friendId", friend.getFriendId());
            item.put("nickname", friend.getNickname());
            item.put("remark", friend.getRemark());
            items.add(item);
        }
        JSONObject result = new JSONObject();
        result.put("total", items.size());
        result.put("friends", items);
        return result.toJSONString();
    }
}
