package com.family.chat.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.family.chat.ChatApplication;
import com.family.chat.api.MessageApi;
import com.family.chat.api.RetrofitClient;
import com.family.chat.constant.MessageType;
import com.family.chat.constant.WsOperationCode;
import com.family.chat.db.AppDatabase;
import com.family.chat.db.LocalMessage;
import com.family.chat.db.MessageDao;
import com.family.chat.model.ChatMessage;
import com.family.chat.network.OssApiClient;
import com.family.chat.network.WebSocketManager;
import com.family.chat.protobuf.MessageProto;
import com.family.chat.util.ChatImageUtil;
import com.family.chat.util.SnowflakeIdGenerator;

public class ChatRepository {

    private static final String TAG = "ChatRepository";

    /** AI 智能助手固定用户ID, 与后端 AI_ASSISTANT_ID 保持一致 */
    public static final long AI_ASSISTANT_ID = 99999L;

    private final MessageApi messageApi;
    private final WebSocketManager webSocketManager;
    private final OssApiClient ossApiClient = new OssApiClient(ChatApplication.getContext());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MessageDao messageDao;

    public interface OnNewMessageListener {
        void onNewMessage(String jsonText);
    }

    public interface ChatCallback<T> {

        void onSuccess(T response);

        void onFailure(String error);
    }

    public ChatRepository() {
        messageApi = RetrofitClient.getInstance().create(MessageApi.class);
        this.webSocketManager = WebSocketManager.getInstance();
        this.messageDao = AppDatabase.getInstance(ChatApplication.getContext()).messageDao();
    }

    public void registerMessageReceiver(OnNewMessageListener listener) {
        webSocketManager.setChatListener(text -> mainHandler.post(() -> {
            if (listener != null) {
                listener.onNewMessage(text);
            }
        }));
    }

    /**
     * 解除聊天页消息监听, 防止页面销毁后监听器继续持有回调导致内存泄漏
     */
    public void unregisterMessageReceiver() {
        webSocketManager.setChatListener(null);
    }

    public void syncDeltaMessages(Long friendId, Long localMaxSeq, final ChatCallback<MessageProto.QueryHistoryResponse> callback) {
        messageApi.syncDeltaMessages(friendId, localMaxSeq).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<MessageProto.QueryHistoryResponse> call, retrofit2.Response<MessageProto.QueryHistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("同步增量记录失败");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<MessageProto.QueryHistoryResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void sendTextMessage(Long receiverId, String textMessage) {
        try {
            Long tempId = SnowflakeIdGenerator.getInstance().nextId();
            LocalMessage localMessage = new LocalMessage(
                    tempId,
                    receiverId,
                    textMessage,
                    System.currentTimeMillis(),
                    true,
                    ChatMessage.STATUS_SENDING,
                    -1L,
                    MessageType.TEXT
            );
            messageDao.insert(localMessage);

            boolean isAiChat = receiverId != null && receiverId == AI_ASSISTANT_ID;
            if (isAiChat) {
                // AI 会话先插入"思考中"占位气泡
                LocalMessage thinkingMessage = new LocalMessage(
                        -1L,
                        receiverId,
                        "...",
                        System.currentTimeMillis() + 1,
                        false,
                        ChatMessage.STATUS_SENDING,
                        -999L,
                        MessageType.TEXT
                );
                messageDao.insert(thinkingMessage);
            }

            int operationCode = isAiChat ? WsOperationCode.AI_MESSAGE : WsOperationCode.TEXT_MESSAGE;
            MessageProto.MsgFrame textMessageProto = MessageProto.MsgFrame.newBuilder()
                    .setTempId(tempId)
                    .setReceiverId(receiverId)
                    .setContent(textMessage)
                    .setType(operationCode)
                    .build();
            webSocketManager.sendBinary(textMessageProto.toByteArray());
        } catch (Exception e) {
            Log.e(TAG, "发送文本消息异常", e);
        }
    }

    public void sendImageMessage(String localPath, Long receiverId) {
        Long tempId = SnowflakeIdGenerator.getInstance().nextId();
        String localCopyPath = ChatImageUtil.makeLocalCopy(localPath);
        LocalMessage localMessage = new LocalMessage(
                tempId,
                receiverId,
                localCopyPath,
                System.currentTimeMillis(),
                true,
                ChatMessage.STATUS_SENDING,
                -1,
                MessageType.IMAGE
        );
        messageDao.insert(localMessage);

        ossApiClient.uploadImage(localPath, new OssApiClient.UploadCallback() {
            @Override
            public void onSuccess(String objectKey, String downloadUrl) {
                // 本地保存 "本地副本路径|签名下载地址", 兼顾离线展示与本地副本丢失时兜底
                String combinedContent = localCopyPath + "|" + downloadUrl;
                messageDao.updateContentAndStatus(tempId, combinedContent, ChatMessage.STATUS_SUCCESS);
                // 消息内容只传对象 key, 由服务端在推送/历史同步出口统一签名, 永不过期
                MessageProto.MsgFrame imageMessageProto = MessageProto.MsgFrame.newBuilder()
                        .setTempId(tempId)
                        .setReceiverId(receiverId)
                        .setContent(objectKey)
                        .setType(WsOperationCode.IMAGE_MESSAGE)
                        .build();
                webSocketManager.sendBinary(imageMessageProto.toByteArray());
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "图片上传失败: " + errorMsg);
                messageDao.updateStatus(localMessage.getMessageId(), ChatMessage.STATUS_FAILED);
            }
        });
    }
}
