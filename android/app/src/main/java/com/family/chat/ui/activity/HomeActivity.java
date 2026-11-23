package com.family.chat.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.family.chat.constant.ServerPushType;
import com.family.chat.config.AppConfig;
import com.family.chat.R;
import com.family.chat.constant.WsOperationCode;
import com.family.chat.databinding.ActivityHomeBinding;
import com.family.chat.db.AppDatabase;
import com.family.chat.db.LocalMessage;
import com.family.chat.db.MessageDao;
import com.family.chat.model.ChatMessage;
import com.family.chat.network.WebSocketManager;
import com.family.chat.protobuf.MessageProto;
import com.family.chat.ui.fragment.ChatFragment;
import com.family.chat.ui.fragment.FriendsFragment;
import com.family.chat.ui.fragment.MeFragment;
import com.family.chat.util.AppExecutors;
import com.family.chat.util.NotificationHelper;
import com.family.chat.util.SessionManager;

import org.json.JSONObject;

import java.util.List;

/** 首页Activity: 底部四Tab + WebSocket 全局消息分发中枢 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private ActivityHomeBinding binding;

    /** 聊天界面 */
    private ChatFragment chatFragment;

    /** 好友界面 */
    private FriendsFragment friendsFragment;

    /** 个人中心界面 */
    private MeFragment meFragment;

    /** 当前显示界面 */
    private Fragment activeFragment;

    /** 界面管理器 */
    private FragmentManager fragmentManager;

    /** Activity创建入口 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestNotificationPermission();
        initFragments(savedInstanceState);
        initTabListeners();
        initWebSocketConnection();

        if (savedInstanceState == null) {
            selectTab(0);
        }
    }

    /**
     * 初始化 WebSocket 长连接与全局消息监听
     */
    private void initWebSocketConnection() {
        WebSocketManager wsManager = WebSocketManager.getInstance();
        wsManager.connect(AppConfig.WS_BASE_URL + "?token=" + SessionManager.getAccessToken());
        wsManager.setGlobalListener(this::handleGlobalWebSocketMessage);
        wsManager.setOnConnectionStateListener(() ->
                runOnUiThread(this::autoResendPendingMessages));
    }

    /**
     * 全局 WebSocket 消息统一分发入口。
     *
     * <p>路由规则与后端下行协议对齐:
     * <ol>
     *     <li>status 非空 → 发送结果 ACK 回执</li>
     *     <li>messageId &gt; 0 → 聊天消息推送 (文本0/图片1/定位3)</li>
     *     <li>type=6 且 messageId=0 → 好友申请红点通知</li>
     *     <li>type=7 且 senderId=0 → 新会话创建通知</li>
     *     <li>type 1~10 → RTC 音视频通话信令</li>
     * </ol>
     */
    private void handleGlobalWebSocketMessage(String jsonText) {
        try {
            JSONObject json = new JSONObject(jsonText);

            // 1. ACK 回执 (二进制回执帧 / AI会话 JSON 回执)
            if (json.has("status")) {
                handleMessageAck(json);
                return;
            }

            int type = json.optInt("type", -1);
            long messageId = json.optLong("messageId", 0L);
            long senderId = json.optLong("senderId", 0L);
            String content = json.optString("content", "");
            String senderName = json.optString("senderName", "");

            // 2. 聊天消息推送: 服务端雪花ID恒大于0
            if (messageId > 0) {
                handleIncomingChatMessage(senderId, senderName, content, type);
                return;
            }

            // 3. 好友申请红点通知 (含上线后的离线补发)
            if (type == ServerPushType.FRIEND_REQUEST_NOTIFY) {
                handleFriendRequestNotify(json, senderName, content);
                return;
            }

            // 4. 新会话创建通知: 刷新会话列表
            if (type == ServerPushType.NEW_SESSION_NOTIFY && senderId == 0) {
                runOnUiThread(() -> {
                    if (activeFragment == chatFragment && chatFragment != null) {
                        chatFragment.loadChatSessions();
                    }
                });
                return;
            }

            // 5. RTC 音视频通话信令 (邀请走 JSON 文本帧, 其余走二进制中转)
            if (type >= WsOperationCode.VOICE_CALL_INVITE && type <= WsOperationCode.VIDEO_CALL_CANCEL) {
                handleRtcCallSignaling(senderId, type, content);
            }
        } catch (Exception e) {
            Log.e(TAG, "解析全局 WebSocket 消息异常", e);
        }
    }

    /**
     * 初始化底部导航栏Tab点击事件
     */
    private void initTabListeners() {
        binding.tabChat.setOnClickListener(v -> selectTab(0));
        binding.tabContacts.setOnClickListener(v -> selectTab(1));
        binding.tabDiscover.setOnClickListener(v -> selectTab(2));
        binding.tabMe.setOnClickListener(v -> selectTab(3));
    }

    /**
     * 根据Tab索引切换底部导航栏页面
     */
    private void selectTab(int index) {
        resetTabStyles();
        updateStatusBarColor(index);
        if (index == 0) {
            binding.ivTabChat.setSelected(true);
            binding.tvTabChat.setTextColor(Color.parseColor("#018941"));
            switchFragment(chatFragment);
            updateToolbarUI(R.id.nav_chat);
        } else if (index == 1) {
            binding.ivTabContacts.setSelected(true);
            binding.tvTabContacts.setTextColor(Color.parseColor("#018941"));
            switchFragment(friendsFragment);
            updateToolbarUI(R.id.nav_contacts);
        } else if (index == 2) {
            binding.ivTabDiscover.setSelected(true);
            binding.tvTabDiscover.setTextColor(Color.parseColor("#018941"));
            Toast.makeText(this, "发现模块建设中...", Toast.LENGTH_SHORT).show();
        } else if (index == 3) {
            binding.ivTabMe.setSelected(true);
            binding.tvTabMe.setTextColor(Color.parseColor("#018941"));
            switchFragment(meFragment);
            updateToolbarUI(R.id.nav_me);
        }
    }

    /**
     * 重置底部导航栏所有Tab的样式为初态
     */
    private void resetTabStyles() {
        int greyColor = Color.parseColor("#1E1E1E");
        binding.ivTabChat.setSelected(false);
        binding.tvTabChat.setTextColor(greyColor);
        binding.ivTabContacts.setSelected(false);
        binding.tvTabContacts.setTextColor(greyColor);
        binding.ivTabDiscover.setSelected(false);
        binding.tvTabDiscover.setTextColor(greyColor);
        binding.ivTabMe.setSelected(false);
        binding.tvTabMe.setTextColor(greyColor);
    }

    /**
     * 处理Activity已存在情况下接收到的新Intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleTargetIntent(intent);
    }

    /**
     * 更新顶栏的样式
     */
    private void updateToolbarUI(int destinationId) {
        binding.homeToolbar.setVisibility(View.VISIBLE);
        if (destinationId == R.id.nav_chat) {
            binding.tvTitle.setText("微信");
        } else if (destinationId == R.id.nav_contacts) {
            binding.tvTitle.setText("通讯录");
        } else {
            binding.homeToolbar.setVisibility(View.GONE);
        }
    }

    /**
     * 更新聊天Tab的未读消息角标
     */
    public void updateChatBadge(int count) {
        runOnUiThread(() -> {
            if (count > 0) {
                binding.tvTabChatBadge.setVisibility(View.VISIBLE);
                binding.tvTabChatBadge.setText(String.valueOf(count));
            } else {
                binding.tvTabChatBadge.setVisibility(View.GONE);
                binding.tvTabChatBadge.setText("");
            }
        });
    }

    private void handleTargetIntent(Intent intent) {
        if (intent != null && "friends_tab".equals(intent.getStringExtra("target_tab"))) {
            selectTab(1);
        } else {
            selectTab(0);
        }
    }

    /** 初始化页面 */
    private void initFragments(Bundle savedInstanceState) {
        fragmentManager = getSupportFragmentManager();
        if (savedInstanceState == null) {
            chatFragment = new ChatFragment();
            friendsFragment = new FriendsFragment();
            meFragment = new MeFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, chatFragment, "ChatFragment")
                    .commit();
            activeFragment = chatFragment;
        } else {
            chatFragment = (ChatFragment) fragmentManager.findFragmentByTag("ChatFragment");
            friendsFragment = (FriendsFragment) fragmentManager.findFragmentByTag("FriendsFragment");
            meFragment = (MeFragment) fragmentManager.findFragmentByTag("MeFragment");
            if (chatFragment != null && chatFragment.isVisible()) activeFragment = chatFragment;
            else if (friendsFragment != null && friendsFragment.isVisible()) activeFragment = friendsFragment;
            else if (meFragment != null && meFragment.isVisible()) activeFragment = meFragment;
        }
    }

    /** 切换页面 */
    private void switchFragment(Fragment targetFragment) {
        if (activeFragment == targetFragment) return;
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (!targetFragment.isAdded()) {
            String tag = targetFragment.getClass().getSimpleName();
            transaction.hide(activeFragment).add(R.id.fragment_container, targetFragment, tag);
        } else {
            transaction.hide(activeFragment).show(targetFragment);
        }
        activeFragment = targetFragment;
        transaction.commit();
    }

    /**
     * Activity销毁时释放WebSocket连接资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        WebSocketManager.getInstance().disconnect();
    }

    /**
     * 离线积压消息自动补发: 按本地消息原始类型重新打包上行
     */
    private void autoResendPendingMessages() {
        AppExecutors.io().execute(() -> {
            MessageDao messageDao = AppDatabase.getInstance(HomeActivity.this).messageDao();
            List<LocalMessage> pendingList = messageDao.getPendingMessages();
            if (pendingList == null || pendingList.isEmpty()) {
                return;
            }
            for (LocalMessage localMessage : pendingList) {
                try {
                    MessageProto.MsgFrame message = MessageProto.MsgFrame.newBuilder()
                            .setTempId(localMessage.getMessageId())
                            .setReceiverId(localMessage.getFriendId())
                            .setContent(localMessage.getContent())
                            .setType(localMessage.getMessageType() != null
                                    ? localMessage.getMessageType() : WsOperationCode.TEXT_MESSAGE)
                            .build();
                    WebSocketManager.getInstance().sendBinary(message.toByteArray());
                } catch (Exception e) {
                    Log.e(TAG, "补发离线消息失败, tempId=" + localMessage.getMessageId(), e);
                }
            }
        });
    }

    /**
     * 重写getResources方法, 屏蔽系统字体缩放对布局的影响
     */
    @Override
    public android.content.res.Resources getResources() {
        android.content.res.Resources resources = super.getResources();
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setToDefaults();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        return resources;
    }

    /**
     * 申请 Android 13+ 必须的通知发送权限
     */
    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 102);
            }
        }
    }

    private void updateStatusBarColor(int index) {
        if (index == 3) {
            binding.getRoot().setBackgroundColor(Color.WHITE);
        } else {
            binding.getRoot().setBackgroundColor(Color.parseColor("#EDEDED"));
        }
    }

    /**
     * 处理好友申请红点通知 (type=6, 含上线后离线补发)
     */
    private void handleFriendRequestNotify(JSONObject json, String senderName, String content) {
        int unreadCount = json.optInt("unreadCount", 1);
        SessionManager.saveUnreadFriendRequestCount(unreadCount);

        runOnUiThread(() -> {
            binding.tvTabContactsBadge.setVisibility(View.VISIBLE);
            binding.tvTabContactsBadge.setText(String.valueOf(unreadCount));
            NotificationHelper.showFriendRequestNotification(HomeActivity.this, senderName, content);
        });
    }

    /**
     * 处理普通的聊天数据包: 非当前聊天窗口的消息弹出通知并刷新会话角标
     */
    private void handleIncomingChatMessage(long senderId, String senderName, String content, int messageType) {
        if (senderId != ChatActivity.activeChatFriendId) {
            String displayPreviewText = formatMessagePreview(content, messageType);

            runOnUiThread(() -> {
                binding.tvTabChatBadge.setVisibility(View.VISIBLE);
                NotificationHelper.showChatNotification(HomeActivity.this, senderId, senderName, displayPreviewText);
            });
        }
        runOnUiThread(() -> {
            if (activeFragment == chatFragment && chatFragment != null) {
                chatFragment.loadChatSessions();
            }
        });
    }

    /**
     * 根据 messageType 格式化通知栏/会话列表的预览文字
     */
    private String formatMessagePreview(String rawContent, int messageType) {
        if (messageType == ServerPushType.IMAGE_MESSAGE) {
            return "[图片]";
        } else if (messageType == ServerPushType.LOCATION_MESSAGE) {
            return "[位置]";
        } else if (messageType == ServerPushType.TEXT_MESSAGE) {
            return rawContent != null ? rawContent : "";
        } else {
            return "[未知格式]";
        }
    }

    /**
     * 处理消息发送 ACK 回执: 更新本地库临时ID为真实消息ID
     */
    private void handleMessageAck(JSONObject json) {
        long tempId = json.optLong("tempId", -1L);
        String status = json.optString("status", "");
        AppExecutors.io().execute(() -> {
            MessageDao messageDao = AppDatabase.getInstance(HomeActivity.this).messageDao();
            if ("success".equals(status)) {
                long sequence = json.optLong("sequence", 0L);
                long realMessageId = json.optLong("messageId", -1L);
                if (realMessageId != -1L) {
                    messageDao.updateRealMsgIdAndStatus(tempId, realMessageId, sequence, ChatMessage.STATUS_SUCCESS);
                }
            } else {
                messageDao.updateStatus(tempId, ChatMessage.STATUS_FAILED);
            }
            runOnUiThread(() -> {
                if (activeFragment == chatFragment && chatFragment != null) {
                    chatFragment.loadChatSessions();
                }
            });
        });
    }

    /**
     * RTC 音视频通话信令分发, 编号与后端 WsOperationCode 对齐 (语音1~5, 视频6~10)
     */
    private void handleRtcCallSignaling(long senderId, int signalingType, String content) {
        runOnUiThread(() -> {
            switch (signalingType) {

                // ==================== 语音通话 (1 ~ 5) ====================

                case WsOperationCode.VOICE_CALL_INVITE: // 收到语音呼叫邀请, content 为声网频道ID
                    Intent audioInvite = new Intent(HomeActivity.this, VoiceCallActivity.class);
                    audioInvite.putExtra("friend_id", senderId);
                    audioInvite.putExtra("is_caller", false);
                    audioInvite.putExtra("channel_id", content);
                    startActivity(audioInvite);
                    break;

                case WsOperationCode.VOICE_CALL_ACCEPT: // 对方接听
                    sendRtcBroadcast("ACTION_AUDIO_CALL_ACCEPTED", senderId);
                    break;

                case WsOperationCode.VOICE_CALL_REFUSE: // 对方拒绝/占线 (服务端离线也回传该信令)
                    Toast.makeText(this, "对方拒绝了语音通话", Toast.LENGTH_SHORT).show();
                    sendRtcBroadcast("ACTION_AUDIO_CALL_REJECTED", senderId);
                    break;

                case WsOperationCode.VOICE_CALL_CANCEL: // 对方响铃中取消
                    Toast.makeText(this, "对方取消了语音呼叫", Toast.LENGTH_SHORT).show();
                    sendRtcBroadcast("ACTION_AUDIO_CALL_CANCELED", senderId);
                    break;

                case WsOperationCode.VOICE_CALL_HANGUP: // 对方通话中挂断
                    Toast.makeText(this, "通话已结束", Toast.LENGTH_SHORT).show();
                    sendRtcBroadcast("ACTION_AUDIO_CALL_HANGUP", senderId);
                    break;

                // ==================== 视频通话 (6 ~ 10) ====================

                case WsOperationCode.VIDEO_CALL_INVITE: // 收到视频通话邀请, content 为声网频道ID
                    Intent videoInvite = new Intent(HomeActivity.this, VideoCallActivity.class);
                    videoInvite.putExtra("friend_id", senderId);
                    videoInvite.putExtra("is_caller", false);
                    videoInvite.putExtra("channel_id", content);
                    startActivity(videoInvite);
                    break;

                case WsOperationCode.VIDEO_CALL_ACCEPT: // 对方接听
                    sendRtcBroadcast("ACTION_VIDEO_CALL_ACCEPTED", senderId);
                    break;

                case WsOperationCode.VIDEO_CALL_REFUSE: // 对方拒绝/占线 (服务端离线也回传该信令)
                    Toast.makeText(this, "对方拒绝了视频通话", Toast.LENGTH_SHORT).show();
                    sendRtcBroadcast("ACTION_VIDEO_CALL_REJECTED", senderId);
                    break;

                case WsOperationCode.VIDEO_CALL_CANCEL: // 对方响铃中取消
                    Toast.makeText(this, "对方取消了视频呼叫", Toast.LENGTH_SHORT).show();
                    sendRtcBroadcast("ACTION_VIDEO_CALL_CANCELED", senderId);
                    break;

                case WsOperationCode.VIDEO_CALL_HANGUP: // 对方通话中挂断
                    Toast.makeText(this, "视频通话已结束", Toast.LENGTH_SHORT).show();
                    sendRtcBroadcast("ACTION_VIDEO_CALL_HANGUP", senderId);
                    break;

                default:
                    Log.w(TAG, "收到未定义的 RTC 信令: " + signalingType);
                    break;
            }
        });
    }

    /**
     * 发送本地广播，通知当前前台正运行的 VoiceCallActivity / VideoCallActivity 关闭或更新状态
     */
    private void sendRtcBroadcast(String action, long senderId) {
        Intent intent = new Intent(action);
        intent.putExtra("sender_id", senderId);
        sendBroadcast(intent);
    }
}
