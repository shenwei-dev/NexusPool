package com.family.chat.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.family.chat.ChatApplication;
import com.family.chat.vo.FriendDetailVO;

import org.json.JSONObject;

/**
 * 好友详情本地缓存 (SharedPreferences + JSON 序列化, 按 friendId 维度存储)。
 *
 * <p>缓存策略 cache-aside: 进入详情页先渲染本地缓存实现秒开与离线可用,
 * 随后网络请求成功再用最新数据覆盖缓存与界面。
 */
public final class FriendDetailCache {

    private static final String TAG = "FriendDetailCache";
    private static final String PREF_NAME = "friend_detail_cache";
    private static final String KEY_PREFIX = "detail_";

    private FriendDetailCache() {}

    /**
     * 保存好友详情到本地缓存
     */
    public static void save(FriendDetailVO detail) {
        if (detail == null || detail.getFriendId() == null) {
            return;
        }
        try {
            JSONObject item = new JSONObject();
            item.put("friendId", detail.getFriendId());
            item.put("nickname", detail.getNickname());
            item.put("avatarUrl", detail.getAvatarUrl());
            item.put("remark", detail.getRemark());
            item.put("account", detail.getAccount());
            getPreferences().edit()
                    .putString(KEY_PREFIX + detail.getFriendId(), item.toString())
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "好友详情缓存写入失败", e);
        }
    }

    /**
     * 从本地缓存读取好友详情, 无缓存返回 null
     */
    public static FriendDetailVO load(Long friendId) {
        if (friendId == null) {
            return null;
        }
        String json = getPreferences().getString(KEY_PREFIX + friendId, "");
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            JSONObject item = new JSONObject(json);
            FriendDetailVO detail = new FriendDetailVO();
            detail.setFriendId(item.optLong("friendId"));
            detail.setNickname(item.optString("nickname"));
            detail.setAvatarUrl(item.optString("avatarUrl"));
            detail.setRemark(item.optString("remark"));
            detail.setAccount(item.optLong("account"));
            return detail;
        } catch (Exception e) {
            Log.e(TAG, "好友详情缓存解析失败", e);
            return null;
        }
    }

    private static SharedPreferences getPreferences() {
        return ChatApplication.getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
