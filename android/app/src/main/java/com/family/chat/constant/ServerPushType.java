package com.family.chat.constant;

/**
 * 服务端下行推送类型常量。
 *
 * <p>与后端 MessageType / FriendRequestServiceImpl 推送约定保持 1:1 对齐。
 */
public final class ServerPushType {

    /** 文本消息推送 */
    public static final int TEXT_MESSAGE = 0;

    /** 图片消息推送 */
    public static final int IMAGE_MESSAGE = 1;

    /** 定位消息推送 */
    public static final int LOCATION_MESSAGE = 3;

    /** 好友申请红点通知 (含离线补发) */
    public static final int FRIEND_REQUEST_NOTIFY = 6;

    /** 新会话创建通知 */
    public static final int NEW_SESSION_NOTIFY = 7;

    private ServerPushType() {
    }
}
