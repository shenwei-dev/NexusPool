package com.family.chat.ai.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiTool;
import com.family.chat.entity.Message;
import com.family.chat.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具: 查询与某位好友的最近聊天记录。
 */
@Component
@RequiredArgsConstructor
public class QueryChatHistoryTool implements AiTool {

    private static final int DEFAULT_LIMIT = 20;

    private static final int MAX_LIMIT = 50;

    private final MessageMapper messageMapper;

    @Override
    public String name() {
        return "query_chat_history";
    }

    @Override
    public String description() {
        return "查询当前用户与某位好友的最近聊天记录(按时间正序返回)。"
                + "用户询问和某人聊过什么、上次聊到哪里、想总结聊天内容时使用";
    }

    @Override
    public JSONObject parametersSchema() {
        JSONObject friendId = new JSONObject();
        friendId.put("type", "integer");
        friendId.put("description", "好友的用户ID");

        JSONObject limit = new JSONObject();
        limit.put("type", "integer");
        limit.put("description", "查询条数, 默认20, 最大50");

        JSONObject properties = new JSONObject();
        properties.put("friendId", friendId);
        properties.put("limit", limit);

        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[]{"friendId"});
        return schema;
    }

    @Override
    public String execute(Long userId, JSONObject args) {
        Long friendId = args.getLong("friendId");
        JSONObject result = new JSONObject();
        if (friendId == null) {
            result.put("total", 0);
            result.put("reason", "缺少参数 friendId");
            return result.toJSONString();
        }

        Integer limitArg = args.getInteger("limit");
        int limit = limitArg == null ? DEFAULT_LIMIT : Math.min(Math.max(limitArg, 1), MAX_LIMIT);

        List<Message> history = messageMapper.selectRecentHistory(userId, friendId, limit);
        JSONArray items = new JSONArray();
        for (Message message : history) {
            JSONObject item = new JSONObject();
            item.put("sender", message.getFromUserId().equals(userId) ? "me" : "friend");
            item.put("content", describeContent(message));
            item.put("time", message.getCreatedAt());
            items.add(item);
        }
        result.put("total", items.size());
        result.put("messages", items);
        return result.toJSONString();
    }

    /** 非文本消息只回传类型占位, 避免把对象 key/坐标串灌进上下文 */
    private String describeContent(Message message) {
        Integer type = message.getMessageType();
        if (type == null || type == 0) {
            return message.getContent();
        }
        return switch (type) {
            case 1 -> "[图片]";
            case 3 -> "[位置]";
            default -> "[其他消息]";
        };
    }
}
