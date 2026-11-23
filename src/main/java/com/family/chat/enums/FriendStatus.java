package com.family.chat.enums;

public enum FriendStatus {

    NORMAL(0, "正常好友"),
    DELETED(1, "已删除");

    private final int code;
    private final String message;

    FriendStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }

    public String getMessage() { return message; }

    public static FriendStatus fromCode(int code) {
        for (FriendStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
