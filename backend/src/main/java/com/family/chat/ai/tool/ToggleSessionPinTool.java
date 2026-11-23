package com.family.chat.ai.tool;

import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiTool;
import com.family.chat.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工具: 会话置顶/取消置顶 (写操作)。
 */
@Component
@RequiredArgsConstructor
public class ToggleSessionPinTool implements AiTool {

    private final ChatSessionMapper chatSessionMapper;

    @Override
    public String name() {
        return "toggle_session_pin";
    }

    @Override
    public String description() {
        return "将与某位好友的会话置顶或取消置顶。参数 friendId 可先通过 list_friends 或 list_chat_sessions 获取。"
                + "仅当用户明确要求置顶/取消置顶某个会话时才调用";
    }

    @Override
    public JSONObject parametersSchema() {
        JSONObject friendId = new JSONObject();
        friendId.put("type", "integer");
        friendId.put("description", "好友的用户ID");

        JSONObject pinned = new JSONObject();
        pinned.put("type", "integer");
        pinned.put("description", "1=置顶, 0=取消置顶");

        JSONObject properties = new JSONObject();
        properties.put("friendId", friendId);
        properties.put("pinned", pinned);

        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[]{"friendId", "pinned"});
        return schema;
    }

    @Override
    public String execute(Long userId, JSONObject args) {
        Long friendId = args.getLong("friendId");
        Integer pinned = args.getInteger("pinned");
        JSONObject result = new JSONObject();
        if (friendId == null || pinned == null || (pinned != 0 && pinned != 1)) {
            result.put("success", false);
            result.put("reason", "参数不合法, 需要 friendId 与 pinned(0或1)");
            return result.toJSONString();
        }

        int affected = chatSessionMapper.updateSessionPinned(userId, friendId, pinned, System.currentTimeMillis());
        result.put("success", affected > 0);
        if (affected == 0) {
            result.put("reason", "与该好友的会话不存在");
        }
        return result.toJSONString();
    }
}
