package com.family.chat.common;

/** 有关状态码的枚举类 */
public enum ResultCode {

    /** 通用模块 */
    SUCCESS(1000, "请求成功"),
    PARAM_ERROR(1001, "参数错误"),
    SYSTEM_ERROR(5000, "系统异常"),

    /** 认证模块 */
    UNAUTHORIZED(1002, "Token失效"),
    INVALID_TOKEN(1003, "Token格式错误"),

    /** 权限模块 */
    FORBIDDEN(1004, "无权限"),

    /** 用户模块 */
    USERNAME_OR_PASSWORD_ERROR(2001, "用户名或密码错误"),
    USERNAME_IS_EXIST(2002, "用户名已存在"),
    USER_BLOCKED(2002, "用户已封禁"),

    /** 好友模块 */
    FRIEND_ALREADY_EXISTS(3001, "好友已存在"),
    FRIEND_REQUEST_PENDING(3002, "好友请求待处理"),
    FRIEND_NOT_FOUND(3003, "好友关系不存在"),

    /** 消息模块 */
    MESSAGE_NOT_FOUND(4001, "消息不存在");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }

    public String getMessage() { return message; }
}
