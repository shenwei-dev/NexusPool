package com.family.chat.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.family.chat.model.ChatMessage;

@Entity(tableName = "local_message")
public class LocalMessage {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "message_id")
    private Long messageId;

    @ColumnInfo(name = "friend_id")
    private Long friendId;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "is_me")
    private boolean isMe;

    @ColumnInfo(name = "send_status")
    private int sendStatus;

    @ColumnInfo(name = "sequence")
    private long sequence;

    @ColumnInfo(name = "message_type")
    private Integer messageType;


    public LocalMessage(@NonNull Long messageId, Long friendId, String content, long timestamp, boolean isMe, int sendStatus, long sequence, Integer messageType) {
        this.messageId = messageId;
        this.friendId = friendId;
        this.content = content;
        this.timestamp = timestamp;
        this.isMe = isMe;
        this.sendStatus = sendStatus;
        this.sequence = sequence;
        this.messageType = messageType;
    }

    @NonNull
    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(@NonNull Long messageId) {
        this.messageId = messageId;
    }

    public Long getFriendId() {
        return friendId;
    }

    public void setFriendId(Long friendId) {
        this.friendId = friendId;
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

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
    }

    public int getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(int sendStatus) {
        this.sendStatus = sendStatus;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public ChatMessage toChatMessage() {
        ChatMessage chatMsg = new ChatMessage();
        chatMsg.setTempId(messageId);
        chatMsg.setReceiverId(friendId);   // 真实 ID// 临时 ID
        chatMsg.setContent(content);      // 内容 (文本/定位JSON/图片URL)
        chatMsg.setTimestamp(timestamp);  // 时间戳
        chatMsg.setSendStatus(sendStatus);
        chatMsg.setMe(isMe);   // 发送状态 (发送中/成功/失败)
        chatMsg.setMessageType(messageType);      // 消息类型 (1文本, 2图片, 3定位)
        return chatMsg;
    }
}