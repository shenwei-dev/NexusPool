package com.family.chat.factory;

import com.family.chat.model.ChatMessage;
import com.family.chat.constant.MessageType;
import com.family.chat.util.SnowflakeIdGenerator;

public class ChatMessageFactory {

    /**
     * 创建【文本消息】
     */
    public static ChatMessage createTextMessage(String content, Long receiverId) {
        ChatMessage message = createBaseMessage(MessageType.TEXT, receiverId);
        message.setContent(content);
        return message;
    }

    /**
     * 基础公共字段赋值 (私有辅助方法)
     */
    private static ChatMessage createBaseMessage(Integer messageType, Long receiverId) {
        ChatMessage message = new ChatMessage();
        message.setTempId(SnowflakeIdGenerator.getInstance().nextId());
        message.setReceiverId(receiverId);
        message.setMessageType(messageType);
        message.setTimestamp(System.currentTimeMillis());
        message.setSendStatus(ChatMessage.STATUS_SENDING);
        message.setMe(true);
        return message;
    }

    /**
     * 创建【图片消息】
     */
    public static ChatMessage createImageMessage(String imageUrl, Long receiverId) {
        ChatMessage message = createBaseMessage(MessageType.IMAGE, receiverId);
        message.setContent(imageUrl);
        return message;
    }
}