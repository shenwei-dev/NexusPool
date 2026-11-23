package com.family.chat.network;

import android.util.Log;

import com.family.chat.constant.WsOperationCode;
import com.family.chat.protobuf.MessageProto;

/**
 * 客户端 RTC 音视频通话信令发送工具类。
 *
 * <p>信令编号与后端 WsOperationCode 保持 1:1 对齐:
 * 语音 1~5, 视频 6~10。对方离线/占线由服务端自动回传拒绝信令, 客户端无需上行 BUSY。
 */
public final class RtcSignalingManager {

    private static final String TAG = "RtcSignalingManager";

    private RtcSignalingManager() {
    }

    // ==================== A. 语音通话信令 ====================

    /** 发起语音呼叫邀请, content 携带声网频道ID */
    public static void sendAudioInvite(Long receiverId, String channelId) {
        sendProtobufSignaling(receiverId, channelId, WsOperationCode.VOICE_CALL_INVITE);
    }

    /** 接受语音通话 (接听) */
    public static void acceptAudioCall(Long receiverId) {
        sendProtobufSignaling(receiverId, "", WsOperationCode.VOICE_CALL_ACCEPT);
    }

    /** 拒绝语音通话 */
    public static void rejectAudioCall(Long receiverId) {
        sendProtobufSignaling(receiverId, "", WsOperationCode.VOICE_CALL_REFUSE);
    }

    /** 取消语音呼叫 (未接通前挂断) */
    public static void cancelAudioCall(Long receiverId) {
        sendProtobufSignaling(receiverId, "", WsOperationCode.VOICE_CALL_CANCEL);
    }

    /** 挂断语音通话 (通话中挂断) */
    public static void hangupAudioCall(Long receiverId) {
        sendProtobufSignaling(receiverId, "", WsOperationCode.VOICE_CALL_HANGUP);
    }

    // ==================== B. 视频通话信令 ====================

    /** 发起视频通话邀请, content 携带声网频道ID */
    public static void sendVideoInvite(Long receiverId, String channelId) {
        sendProtobufSignaling(receiverId, channelId, WsOperationCode.VIDEO_CALL_INVITE);
    }

    /** 接受视频通话 (接听) */
    public static void acceptVideoCall(Long receiverId) {
        sendProtobufSignaling(receiverId, "", WsOperationCode.VIDEO_CALL_ACCEPT);
    }

    /** 拒绝视频通话 */
    public static void rejectVideoCall(Long receiverId) {
        sendProtobufSignaling(receiverId, "", WsOperationCode.VIDEO_CALL_REFUSE);
    }

    /** 取消视频呼叫 (未接通前挂断) */
    public static void cancelVideoCall(Long receiverId) {
        sendProtobufSignaling(receiverId, "", WsOperationCode.VIDEO_CALL_CANCEL);
    }

    /** 挂断视频通话 (通话中挂断) */
    public static void hangupVideoCall(Long receiverId) {
        sendProtobufSignaling(receiverId, "", WsOperationCode.VIDEO_CALL_HANGUP);
    }

    // ==================== 统一底层 Protobuf 二进制打包发送 ====================

    private static void sendProtobufSignaling(Long receiverId, String content, int operationCode) {
        try {
            MessageProto.MsgFrame frame = MessageProto.MsgFrame.newBuilder()
                    .setReceiverId(receiverId)
                    .setContent(content)
                    .setType(operationCode)
                    .build();
            WebSocketManager.getInstance().sendBinary(frame.toByteArray());
        } catch (Exception e) {
            Log.e(TAG, "RTC 信令打包发送失败, operationCode=" + operationCode, e);
        }
    }
}
