package com.family.chat.ai.tool;

import com.alibaba.fastjson.JSONObject;
import com.family.chat.ai.AiTool;
import com.family.chat.entity.User;
import com.family.chat.mapper.FriendMapper;
import com.family.chat.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工具: 按账号搜索用户 (用于查找还不是好友的用户)。
 */
@Component
@RequiredArgsConstructor
public class SearchUserTool implements AiTool {

    private final UserMapper userMapper;

    private final FriendMapper friendMapper;

    @Override
    public String name() {
        return "search_user_by_account";
    }

    @Override
    public String description() {
        return "按 FamilyChat 账号(纯数字)精确搜索用户, 返回该用户的 userId 与昵称, 并标明是否已是好友。"
                + "用户想查找某人、或要给某个还不是好友的账号发好友申请时使用";
    }

    @Override
    public JSONObject parametersSchema() {
        JSONObject account = new JSONObject();
        account.put("type", "integer");
        account.put("description", "要搜索的用户账号(数字)");

        JSONObject properties = new JSONObject();
        properties.put("account", account);

        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[]{"account"});
        return schema;
    }

    @Override
    public String execute(Long userId, JSONObject args) {
        Long account = args.getLong("account");
        JSONObject result = new JSONObject();
        if (account == null) {
            result.put("found", false);
            result.put("reason", "缺少参数 account");
            return result.toJSONString();
        }

        User user = userMapper.selectByAccount(account);
        if (user == null || (user.getDeleted() != null && user.getDeleted())) {
            result.put("found", false);
            result.put("reason", "未找到该账号对应的用户");
            return result.toJSONString();
        }

        result.put("found", true);
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        result.put("account", user.getAccount());
        result.put("isFriend", friendMapper.exists(userId, user.getId()) > 0);
        return result.toJSONString();
    }
}
