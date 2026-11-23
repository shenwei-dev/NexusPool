package com.family.chat.vo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatSessionVO {

    private Long sessionId;
    private Long friendId;
    private String displayName;
    private String avatarUrl;
    private String lastMessage;
    private Integer unreadCount;
    private Long updatedAt;
    /** 后端 ChatSessionVO 字段名为 pinned */
    private Integer pinned;
    public Integer getPinned() { return pinned; }
    public void setPinned(Integer pinned) { this.pinned = pinned; }

    // Getters and Setters...
    public String getDisplayName() { return displayName; }
    public String getLastMessage() { return lastMessage; }
    public Integer getUnreadCount() { return unreadCount; }
    public Long getUpdatedAt() { return updatedAt; }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getFriendId() {
        return friendId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * 微信高保真时间转化
     */
    public String getFormatTime() {
        if (updatedAt == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(updatedAt));
    }
}