package com.family.chat.ai.tool;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiTool;
import com.family.chat.mapper.ChatSessionMapper;
import com.family.chat.vo.ChatSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具: 查询当前用户的会话列表 (含未读数)。
 */
@Component
@RequiredArgsConstructor
public class ListChatSessionsTool implements AiTool {

    /** 会话列表截断条数, 控制回传 token 规模 */
    private static final int MAX_SESSIONS = 20;

    private final ChatSessionMapper chatSessionMapper;

    @Override
    public String name() {
        return "list_chat_sessions";
    }

    @Override
    public String description() {
        return "查询当前用户的会话列表(按置顶与活跃时间排序), 返回对方ID(friendId)、展示名、最后一条消息与未读数。"
                + "用户询问最近聊天、谁给他发过消息、有哪些未读消息时使用";
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
        List<ChatSessionVO> sessions = chatSessionMapper.selectUserSessions(userId);
        JSONArray items = new JSONArray();
        int shown = 0;
        for (ChatSessionVO session : sessions) {
            if (shown >= MAX_SESSIONS) {
                break;
            }
            JSONObject item = new JSONObject();
            item.put("friendId", session.getFriendId());
            item.put("displayName", session.getDisplayName());
            item.put("unreadCount", session.getUnreadCount());
            item.put("lastMessage", session.getLastMessage());
            item.put("pinned", session.getPinned());
            items.add(item);
            shown++;
        }
        JSONObject result = new JSONObject();
        result.put("total", sessions.size());
        result.put("sessions", items);
        return result.toJSONString();
    }
}
