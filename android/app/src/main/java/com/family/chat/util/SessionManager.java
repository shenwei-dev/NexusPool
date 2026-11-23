package com.family.chat.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.family.chat.ChatApplication;

public final class SessionManager {

    private static final String PREF_NAME = "user_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_UNREAD_CHAT_MESSAGE_COUNT = "unread_chat_message_count";
    private static final String KEY_UNREAD_FRIEND_REQUEST_COUNT = "unread_friend_request_count";
    private static final String KEY_LOCAL_AVATAR_PATH = "local_avatar_path";
    private static final String KEY_OSS_AVATAR_URL = "oss_avatar_url";

    private static volatile SharedPreferences preferences;

    private SessionManager() {}

    private static Context getContext() {
        return ChatApplication.getContext();
    }

    /**
     * 获取 SharedPreferences
     */
    private static SharedPreferences getPreferences() {
        if (preferences == null) {
            synchronized (SessionManager.class) {
                if (preferences == null) {
                    preferences = getContext()
                            .getSharedPreferences(
                                    PREF_NAME,
                                    Context.MODE_PRIVATE
                            );
                }
            }
        }
        return preferences;
    }

    /**
     * 保存用户登录信息
     */
    public static void saveSession(
            String accessToken,
            String refreshToken,
            long userId,
            long account,
            String nickname
    ) {
        getPreferences()
                .edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_USER_ID, userId)
                .putLong(KEY_ACCOUNT, account)
                .putString(KEY_NICKNAME, nickname)
                .apply();
    }

    /**
     * 获取 Access Token
     */
    public static String getAccessToken() {
        return getPreferences()
                .getString(KEY_ACCESS_TOKEN, "");
    }

    /**
     * 获取 Refresh Token
     */
    public static String getRefreshToken() {
        return getPreferences()
                .getString(KEY_REFRESH_TOKEN, "");
    }

    /**
     * 获取用户 ID
     */
    public static long getUserId() {
        return getPreferences()
                .getLong(KEY_USER_ID, 0L);
    }

    /**
     * 获取账号
     */
    public static long getAccount() {
        return getPreferences()
                .getLong(KEY_ACCOUNT, 0L);
    }

    /**
     * 获取昵称
     */
    public static String getNickname() {
        return getPreferences()
                .getString(KEY_NICKNAME, "");
    }



    /**
     * 清除 Access Token
     */
    public static void clearAccessToken() {
        getPreferences()
                .edit()
                .remove(KEY_ACCESS_TOKEN)
                .apply();
    }

    /**
     * 判断本地是否有accessToken
     */
    public static boolean hasAccessToken() {
        return !getAccessToken().isEmpty();
    }

    /**
     * 退出登录，清除全部会话信息
     */
    public static void clearSession() {
        getPreferences()
                .edit()
                .clear()
                .apply();
    }

    public static void saveAccessToken(String accessToken) {
        getPreferences()
                .edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .apply();
    }

    public static void saveRefreshToken(String refreshToken) {
        getPreferences()
                .edit()
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public static void saveAvatar(
            String localAvatarPath,
            String ossAvatarUrl
    ) {
        getPreferences()
                .edit()
                .putString(KEY_LOCAL_AVATAR_PATH, localAvatarPath)
                .putString(KEY_OSS_AVATAR_URL, ossAvatarUrl)
                .apply();
    }

    /**
     * 获取本地头像地址
     */
    public static String getLocalAvatarPath() {
        return getPreferences()
                .getString(KEY_LOCAL_AVATAR_PATH, "");
    }

    /**
     * 获取OSS头像地址
     */
    public static String getOssAvatarUrl() {
        return getPreferences()
                .getString(KEY_OSS_AVATAR_URL, "");
    }

    public static void saveLocalAvatarPath(
            String localAvatarPath
    ) {
        getPreferences()
                .edit()
                .putString(KEY_LOCAL_AVATAR_PATH, localAvatarPath)
                .apply();
    }

    public static void saveUnreadChatMessageCount(int count) {
        getPreferences()
                .edit()
                .putInt(KEY_UNREAD_CHAT_MESSAGE_COUNT, count)
                .apply();
    }

    public static void saveUnreadFriendRequestCount(int count) {
        getPreferences()
                .edit()
                .putInt(KEY_UNREAD_FRIEND_REQUEST_COUNT, count)
                .apply();
    }

    public static int getUnreadChatMessageCount() {
        return getPreferences()
                .getInt(KEY_UNREAD_CHAT_MESSAGE_COUNT, 0);
    }

    public static int getUnreadFriendRequestCount() {
        return getPreferences()
                .getInt(KEY_UNREAD_FRIEND_REQUEST_COUNT, 0);
    }

    public static void saveNickname(String nickname) {
        getPreferences()
                .edit()
                .putString(KEY_NICKNAME, nickname)
                .apply();
    }
}

