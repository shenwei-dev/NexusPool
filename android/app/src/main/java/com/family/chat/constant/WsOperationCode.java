package com.family.chat.constant;

/**
 * WebSocket 客户端上行操作码常量。
 *
 * <p>与后端 WsOperationCode 保持 1:1 对齐, 禁止在业务代码中直接使用魔法数字。
 */
public final class WsOperationCode {

    /** 普通文本消息 */
    public static final int TEXT_MESSAGE = 0;

    /** 发起语音通话呼叫 */
    public static final int VOICE_CALL_INVITE = 1;

    /** 接受语音通话 */
    public static final int VOICE_CALL_ACCEPT = 2;

    /** 挂断语音通话 */
    public static final int VOICE_CALL_HANGUP = 3;

    /** 拒绝语音通话邀请 */
    public static final int VOICE_CALL_REFUSE = 4;

    /** 取消语音通话呼叫 */
    public static final int VOICE_CALL_CANCEL = 5;

    /** 发起视频通话呼叫 */
    public static final int VIDEO_CALL_INVITE = 6;

    /** 接受视频通话 */
    public static final int VIDEO_CALL_ACCEPT = 7;

    /** 挂断视频通话 */
    public static final int VIDEO_CALL_HANGUP = 8;

    /** 拒绝视频通话邀请 */
    public static final int VIDEO_CALL_REFUSE = 9;

    /** 取消视频通话呼叫 */
    public static final int VIDEO_CALL_CANCEL = 10;

    /** 位置消息 */
    public static final int LOCATION_MESSAGE = 11;

    /** 图片消息 */
    public static final int IMAGE_MESSAGE = 12;

    /** AI 助理消息 */
    public static final int AI_MESSAGE = 13;

    /** 客户端心跳 */
    public static final int HEARTBEAT = 99;

    private WsOperationCode() {
    }
}
