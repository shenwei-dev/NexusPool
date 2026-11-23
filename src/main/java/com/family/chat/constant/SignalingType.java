package com.family.chat.constant;

public class SignalingType {

    public static final int MSG_PULL   = 1001;
    public static final int AI_MSG_PULL   = 1002;

    // ================= 1. 纯语音通话信令 (1100 - 1199) =================
    public static final int AUDIO_CALL_INVITE   = 1101; // 发起【纯语音】呼叫邀请
    public static final int AUDIO_CALL_ACCEPT   = 1102; // 接受【纯语音】通话 (接听)
    public static final int AUDIO_CALL_REJECT   = 1103; // 拒绝【纯语音】通话
    public static final int AUDIO_CALL_CANCEL   = 1104; // 取消【纯语音】呼叫 (主叫方未接通前挂断)
    public static final int AUDIO_CALL_HANGUP   = 1105; // 挂断【纯语音】通话 (通话中途挂断)
    public static final int AUDIO_CALL_BUSY     = 1106; // 对方占线中 (纯语音)
    public static final int AUDIO_CALL_TIMEOUT  = 1107; // 纯语音 60 秒超时无人接听 (自动挂断)

    // ================= 2. 视频通话信令 (1200 - 1299) =================
    public static final int VIDEO_CALL_INVITE   = 1201; // 发起【视频通话】邀请
    public static final int VIDEO_CALL_ACCEPT   = 1202; // 接受【视频通话】 (接听)
    public static final int VIDEO_CALL_REJECT   = 1203; // 拒绝【视频通话】
    public static final int VIDEO_CALL_CANCEL   = 1204; // 取消【视频通话】呼叫 (主叫方未接通前挂断)
    public static final int VIDEO_CALL_HANGUP   = 1205; // 挂断【视频通话】 (通话中途挂断)
    public static final int VIDEO_CALL_BUSY     = 1206; // 对方占线中 (视频)
    public static final int VIDEO_CALL_TIMEOUT  = 1207; // 视频 60 秒超时无人接听 (自动挂断)
    public static final int VIDEO_TO_AUDIO      = 1208; // 🌟 微信同款：视频通话中途切换为【纯语音模式】

    // ================= 3. 消息与会话控制信令 (2000 - 2999) =================
    public static final int MSG_REVOKE          = 2001; // 消息撤回信令
    public static final int MSG_READ_ACK        = 2002; // 消息已读回执信令
    public static final int MSG_TYPING          = 2003; // 对方正在输入中...

    // ================= 4. 好友关系链信令 (3000 - 3999) =================
    public static final int FRIEND_APPLY        = 3001; // 收到好友申请通知
    public static final int FRIEND_ACCEPT       = 3002; // 通过好友申请通知
    public static final int FRIEND_DELETE       = 3003; // 被对方删除好友通知

    // ================= 5. 系统与设备控制信令 (4000 - 4999) =================
    public static final int FORCE_LOGOUT        = 4001; // 异地登录/设备被挤下线通知
    public static final int HEARTBEAT_PING      = 4002; // 客户端心跳 Ping
    public static final int HEARTBEAT_PONG      = 4003; // 服务端心跳 Pong
}