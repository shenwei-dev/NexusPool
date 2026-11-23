package com.family.chat.enums;

public enum MessageStatus {

    UNREAD(0, "未读"),
    READ(1, "已读");

    private final int code;
    private final String message;

    MessageStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }

    public String getMessage() { return message; }

    public static MessageStatus fromCode(int code) {
        for (MessageStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
