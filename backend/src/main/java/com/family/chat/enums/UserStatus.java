package com.family.chat.enums;

public enum UserStatus {

    NORMAL(0, "正常"),
    BANNED(1, "封禁");

    private final int code;
    private final String message;

    UserStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }

    public String getMessage() { return message; }

    public static UserStatus fromCode(int code) {
        for (UserStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
