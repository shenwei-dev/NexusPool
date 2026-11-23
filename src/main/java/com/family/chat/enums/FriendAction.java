package com.family.chat.enums;

public enum FriendAction {

    SEND_REQUEST(1, "发送好友请求"),
    ACCEPT(2, "同意好友请求"),
    REJECT(3, "拒绝好友请求"),
    DELETE(4, "删除好友"),
    BLOCK(5, "拉黑"),
    UNBLOCK(6, "解拉黑");

    private final int code;
    private final String message;

    FriendAction(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }

    public String getMessage() { return message; }

    public static FriendAction fromCode(int code) {
        for (FriendAction action : values()) {
            if (action.code == code) return action;
        }
        return null;
    }
}
