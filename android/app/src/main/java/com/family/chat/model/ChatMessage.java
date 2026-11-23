package com.family.chat.model;

import android.util.Log;

public class ChatMessage {

    /** 消息ID */
    private Long tempId;

    /** 接收者ID */
    private Long receiverId;

    /** 消息内容 */
    private String content;

    /** 消息发送的时间戳 */
    private long timestamp;

    /** 消息发送状态 */
    private int sendStatus;

    /** 是否由当前登录账户发送 */
    private boolean isMe;

    /** 消息类型 */
    private Integer messageType;

    public static final int STATUS_SENDING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;

    public ChatMessage() {}

    public ChatMessage(Long tempId, Long receiverId, String content, long timestamp, int sendStatus, boolean isMe, Integer messageType) {
        this.tempId = tempId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.sendStatus = sendStatus;
        this.isMe = isMe;
        this.messageType = messageType;
    }

    public Long getTempId() {
        return tempId;
    }

    public void setTempId(Long tempId) {
        this.tempId = tempId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(int sendStatus) {
        this.sendStatus = sendStatus;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    /**
     * 快捷将当前消息内容转换为 LocationMessage
     */
    public LocationMessage toLocationMessage() {
        try {
            return new com.google.gson.Gson().fromJson(this.content, LocationMessage.class);
        } catch (Exception e) {
            Log.w("ChatMessage", "位置消息内容解析失败: " + e.getMessage());
            return new LocationMessage("", "", "",0, 0);
        }
    }
}