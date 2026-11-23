package com.family.chat.enums;

public enum FriendRequestStatus {

    PENDING(0, "待处理"),
    ACCEPTED(1, "已同意"),
    REJECTED(2, "已拒绝"),
    EXPIRED(3, "已过期");

    private final int code;
    private final String message;

    FriendRequestStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }

    public String getMessage() { return message; }

    public static FriendRequestStatus fromCode(int code) {
        for (FriendRequestStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
