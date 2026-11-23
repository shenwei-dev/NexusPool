package com.family.chat.enums;

public enum PrivacyMode {

    PUBLIC(0, "公开"),
    PRIVATE(1, "隐私");

    private final int code;
    private final String message;

    PrivacyMode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }

    public String getMessage() { return message; }

    public static PrivacyMode fromCode(int code) {
        for (PrivacyMode mode : values()) {
            if (mode.code == code) return mode;
        }
        return null;
    }
}
