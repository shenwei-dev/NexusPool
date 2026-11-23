package com.family.chat.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.family.chat.ChatApplication;
import com.family.chat.vo.FriendVO;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 好友列表本地缓存 (SharedPreferences + JSON 序列化)。
 *
 * <p>缓存策略 cache-aside: 进入联系人页先渲染本地缓存秒开列表,
 * 随后网络请求成功再用最新数据覆盖缓存与界面。
 */
public final class FriendListCache {

    private static final String TAG = "FriendListCache";
    private static final String PREF_NAME = "friend_list_cache";
    private static final String KEY_FRIEND_LIST = "friend_list_json";

    private FriendListCache() {}

    /**
     * 保存好友列表到本地缓存
     */
    public static void save(List<FriendVO> friends) {
        if (friends == null) {
            return;
        }
        try {
            JSONArray array = new JSONArray();
            for (FriendVO friend : friends) {
                JSONObject item = new JSONObject();
                item.put("friendId", friend.getFriendId());
                item.put("nickname", friend.getNickname());
                item.put("avatarUrl", friend.getAvatarUrl());
                item.put("remark", friend.getRemark());
                array.put(item);
            }
            getPreferences().edit().putString(KEY_FRIEND_LIST, array.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "好友列表缓存写入失败", e);
        }
    }

    /**
     * 从本地缓存读取好友列表, 无缓存返回空集合
     */
    public static List<FriendVO> load() {
        List<FriendVO> result = new ArrayList<>();
        String json = getPreferences().getString(KEY_FRIEND_LIST, "");
        if (TextUtils.isEmpty(json)) {
            return result;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                FriendVO friend = new FriendVO();
                friend.setFriendId(item.optLong("friendId"));
                friend.setNickName(item.optString("nickname"));
                friend.setAvatarUrl(item.optString("avatarUrl"));
                friend.setRemark(item.optString("remark"));
                result.add(friend);
            }
        } catch (Exception e) {
            Log.e(TAG, "好友列表缓存解析失败", e);
        }
        return result;
    }

    private static SharedPreferences getPreferences() {
        return ChatApplication.getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
