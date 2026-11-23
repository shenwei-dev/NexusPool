package com.family.chat.common.result;

public enum ResultCode {

    SUCCESS(1000, "请求成功"),

    // 通用错误
    PARAM_ERROR(1001, "参数错误"),
    UNAUTHORIZED(1002, "Token失效"),
    INVALID_TOKEN(1003, "Token格式错误"),
    FORBIDDEN(1004, "无权限"),

    // 用户模块
    USERNAME_OR_PASSWORD_ERROR(2001, "用户名或密码错误"),
    USERNAME_IS_EXIST(2002, "用户名已存在"),
    USER_BLOCKED(2003, "用户已封禁"),
    USER_NOT_EXIST(2004, "用户不存在"),

    // 好友模块
    FRIEND_ALREADY_EXISTS(3001, "好友已存在"),
    FRIEND_REQUEST_PENDING(3002, "好友请求待处理"),
    FRIEND_NOT_FOUND(3003, "好友关系不存在"),
    FRIEND_REQUEST_NOT_EXIST(3004, "好友请求不存在"),

    // 消息模块
    MESSAGE_NOT_FOUND(4001, "消息不存在"),

    FILE_IS_NULL(5001, "文件为空"),

    // 认证模块
    REGISTRATION_BUSY_RETRY(6001, "注册繁忙，请重试"),

    SYSTEM_ERROR(9999, "系统异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }

    public String getMessage() { return message; }
}
