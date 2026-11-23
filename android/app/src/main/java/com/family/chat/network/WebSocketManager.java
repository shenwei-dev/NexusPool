package com.family.chat.network;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;

import com.family.chat.constant.WsOperationCode;
import com.family.chat.protobuf.MessageProto;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * WebSocket 长连接管理器 (单例)。
 *
 * <p>协议约定与后端保持一致:
 * <ul>
 *     <li>上行心跳: 二进制 MsgFrame, type=99</li>
 *     <li>下行二进制帧: MsgFrame Protobuf, 统一转换为 JSON 字符串分发</li>
 *     <li>下行文本帧: 服务端 JSON 信令 (呼叫邀请 / AI 会话 ACK), 原样分发</li>
 * </ul>
 */
public class WebSocketManager {

    private static final String TAG = "WebSocketManager";
    private static final int HEARTBEAT_INTERVAL_MS = 30000;
    private static final int RECONNECT_DELAY_MS = 5000;

    private static volatile WebSocketManager instance;

    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client;

    /** volatile 保证跨线程可见性 (OkHttp 回调线程与主线程并发访问) */
    private volatile WebSocket mWebSocket;

    private OnMessageReceivedListener globalListener;
    private OnMessageReceivedListener chatListener;
    private OnConnectionStateListener connectionStateListener;

    private String mUrl;
    private boolean isManualClose = false;

    public interface OnMessageReceivedListener {
        void onMessageReceived(String text);
    }

    public interface OnConnectionStateListener {
        void onConnected();
    }

    public void setOnConnectionStateListener(OnConnectionStateListener listener) {
        this.connectionStateListener = listener;
    }

    private WebSocketManager() {
        // 开发环境服务端使用自签名证书, 此处信任全部证书; 生产环境必须替换为证书固定(Certificate Pinning)
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            client = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .pingInterval(0, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "构建 SSL 客户端发生异常", e);
            throw new IllegalStateException("初始化 WebSocket SSL 上下文失败", e);
        }
    }

    public static WebSocketManager getInstance() {
        if (instance == null) {
            synchronized (WebSocketManager.class) {
                if (instance == null) {
                    instance = new WebSocketManager();
                }
            }
        }
        return instance;
    }

    public void setGlobalListener(OnMessageReceivedListener listener) {
        this.globalListener = listener;
    }

    public void setChatListener(OnMessageReceivedListener listener) {
        this.chatListener = listener;
    }

    /**
     * 建立连接
     */
    public synchronized void connect(String url) {
        this.mUrl = url;
        this.isManualClose = false;

        if (mWebSocket != null) {
            Log.d(TAG, "WebSocket 已经连接，无需重复连接");
            return;
        }

        Request request = new Request.Builder().url(url).build();
        mWebSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "WebSocket 物理长连接建立成功");
                reconnectHandler.removeCallbacks(reconnectRunnable);
                heartbeatHandler.removeCallbacks(heartbeatRunnable);
                heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
                if (connectionStateListener != null) {
                    connectionStateListener.onConnected();
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                try {
                    MessageProto.MsgFrame protoMsg = MessageProto.MsgFrame.parseFrom(bytes.toByteArray());
                    dispatchBinaryFrame(protoMsg);
                } catch (Exception e) {
                    Log.e(TAG, "解析 Protobuf 二进制帧失败", e);
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                // 服务端 JSON 文本帧: 呼叫邀请信令 / AI 会话 ACK, 直接透传分发
                dispatch(text);
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Log.d(TAG, "WebSocket 已安全关闭: " + reason);
                mWebSocket = null;
                heartbeatHandler.removeCallbacks(heartbeatRunnable);
                tryReconnect();
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                Log.e(TAG, "WebSocket 发生连接异常或网络断开: " + t.getMessage());
                mWebSocket = null;
                heartbeatHandler.removeCallbacks(heartbeatRunnable);
                tryReconnect();
            }
        });
    }

    /**
     * 将二进制 MsgFrame 转换为统一 JSON 字符串后分发, 供上层按字段特征路由
     */
    private void dispatchBinaryFrame(MessageProto.MsgFrame protoMsg) {
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            if (!TextUtils.isEmpty(protoMsg.getStatus())) {
                // ACK 回执帧
                json.put("status", protoMsg.getStatus());
                json.put("tempId", protoMsg.getTempId());
                json.put("senderId", protoMsg.getSenderId());
                json.put("messageId", protoMsg.getMessageId());
                json.put("sequence", protoMsg.getSequence());
            } else {
                // 业务推送帧 (聊天消息 / RTC 信令 / 好友红点 / 新会话通知)
                json.put("type", protoMsg.getType());
                json.put("senderId", protoMsg.getSenderId());
                json.put("receiverId", protoMsg.getReceiverId());
                json.put("messageId", protoMsg.getMessageId());
                json.put("content", protoMsg.getContent());
                json.put("timestamp", protoMsg.getTimestamp());
                json.put("sequence", protoMsg.getSequence());
                json.put("senderName", protoMsg.getSenderName());
                json.put("senderAvatarUrl", protoMsg.getSenderAvatarUrl());
                json.put("unreadCount", protoMsg.getUnreadCount());
                json.put("lastMessage", protoMsg.getLastMessage());
            }
            dispatch(json.toString());
        } catch (Exception e) {
            Log.e(TAG, "分发二进制帧失败", e);
        }
    }

    private void dispatch(String jsonText) {
        if (chatListener != null) {
            chatListener.onMessageReceived(jsonText);
        }
        if (globalListener != null) {
            globalListener.onMessageReceived(jsonText);
        }
    }

    /**
     * 自动重连控制逻辑: 非手动断开时延迟重连, 防止重连风暴
     */
    private void tryReconnect() {
        if (isManualClose) {
            Log.d(TAG, "用户手动断开连接或已登出，停止自动重连");
            return;
        }
        reconnectHandler.removeCallbacks(reconnectRunnable);
        reconnectHandler.postDelayed(reconnectRunnable, RECONNECT_DELAY_MS);
    }

    private final Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "正在发起自动重连...");
            if (mUrl != null) {
                connect(mUrl);
            }
        }
    };

    /**
     * 手动/正常断开连接 (如用户退出登录)
     */
    public void disconnect() {
        Log.d(TAG, "调用了手动断开连接");
        this.isManualClose = true;
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
        reconnectHandler.removeCallbacks(reconnectRunnable);

        WebSocket socket = mWebSocket;
        mWebSocket = null;
        if (socket != null) {
            socket.close(1000, "Normal closure");
        }
    }

    /**
     * 发送文本消息 (WebSocket 文本帧)
     *
     * @return true 代表已成功写入发送缓冲区
     */
    public boolean sendText(String jsonText) {
        WebSocket socket = mWebSocket;
        if (socket != null && jsonText != null && !jsonText.isEmpty()) {
            boolean isQueued = socket.send(jsonText);
            if (!isQueued) {
                Log.e(TAG, "发送失败：WebSocket 缓冲区满或连接已断开");
            }
            return isQueued;
        }
        Log.e(TAG, "发送失败：WebSocket 未建立连接，或发送文本为空");
        return false;
    }

    /**
     * 发送二进制数据 (WebSocket 二进制帧, Protobuf 打包)
     *
     * @return true 代表已成功写入发送缓冲区
     */
    public boolean sendBinary(byte[] bytes) {
        WebSocket socket = mWebSocket;
        if (socket != null && bytes != null && bytes.length > 0) {
            boolean isQueued = socket.send(ByteString.of(bytes));
            if (!isQueued) {
                Log.e(TAG, "发送失败：WebSocket 缓冲区满或连接已断开");
            }
            return isQueued;
        }
        Log.e(TAG, "发送失败：WebSocket 未建立连接，或数据为空");
        return false;
    }

    /**
     * 应用层心跳: 二进制 MsgFrame(type=99), 与后端 ChatHandler 二进制帧处理链路对齐
     */
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                MessageProto.MsgFrame heartbeat = MessageProto.MsgFrame.newBuilder()
                        .setType(WsOperationCode.HEARTBEAT)
                        .build();
                sendBinary(heartbeat.toByteArray());
            } catch (Exception e) {
                Log.e(TAG, "心跳包发送异常", e);
            }
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
        }
    };
}
