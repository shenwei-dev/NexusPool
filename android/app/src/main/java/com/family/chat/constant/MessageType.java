package com.family.chat.constant;

public class MessageType {

    // ===== 基础消息类型 =====
    public static final int TEXT = 0;         // 文本消息
    public static final int IMAGE = 1;        // 图片消息
    public static final int VIDEO = 2;        // 短视频消息
    public static final int LOCATION = 3;     // 定位消息
    public static final int FILE = 4;         // 文件消息
    public static final int VOICE = 5;        // 语音消息/语音条

    // ===== 扩展/系统消息类型 =====
    public static final int REALTIME_LOCATION = 101; // 实时位置共享通知

    /**
     * 判断是否为媒体类型 (用于下载/缓存判断)
     */
    public static boolean isMediaMessage(int msgType) {
        return msgType == IMAGE || msgType == VOICE || msgType == VIDEO || msgType == FILE;
    }
}