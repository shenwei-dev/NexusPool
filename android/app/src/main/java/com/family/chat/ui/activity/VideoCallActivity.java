package com.family.chat.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.family.chat.R;
import com.family.chat.network.RtcSignalingManager;
import com.family.chat.util.AvatarManager;
import com.family.chat.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import java.util.Locale;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class VideoCallActivity extends AppCompatActivity {

    private static final String TAG = "VideoCallActivity";
    private static final String AGORA_APP_ID = com.family.chat.BuildConfig.AGORA_APP_ID;

    // 🌟 状态机定义
    private static final int STATE_CALLING = 1; // 主叫：等待接听
    private static final int STATE_RINGING = 2; // 被叫：收到视频来电
    private static final int STATE_TALKING = 3; // 双方：视频通话中

    private int currentState = STATE_CALLING;

    private TextView tvCallStatus;
    private ImageView ivCallAvatar;
    private FrameLayout containerLocalVideo;
    private FrameLayout containerRemoteVideo;

    private LinearLayout layoutRingingPanel;
    private MaterialButton btnDecline;
    private MaterialButton btnAccept;

    private LinearLayout layoutTalkingPanel;
    private MaterialButton btnMuteMic;
    private MaterialButton btnHangup;
    private MaterialButton btnSwitchCamera;
    private TextView tvMicStatus;
    private TextView tvHangupLabel;

    private Long friendId;
    private boolean isCaller;
    private RtcEngine mRtcEngine;

    private boolean isMicMuted = false;
    private Vibrator vibrator;
    private Ringtone ringtone;

    // 通话计时器
    private int callDurationSeconds = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        friendId = getIntent().getLongExtra("friend_id", -1L);
        isCaller = getIntent().getBooleanExtra("is_caller", false);

        requestPermissions(); // 申请麦克风 + 相机双重权限
        initView();
        initCallState();
        registerRtcReceiver(); // 注册本地信令广播监听
    }

    private void initView() {
        tvCallStatus = findViewById(R.id.tv_call_status);
        ivCallAvatar = findViewById(R.id.iv_call_avatar);
        containerLocalVideo = findViewById(R.id.container_local_video);
        containerRemoteVideo = findViewById(R.id.container_remote_video);

        // 接通前展示对方真实头像
        AvatarManager.load(ivCallAvatar, friendId);

        layoutRingingPanel = findViewById(R.id.layout_ringing_panel);
        btnDecline = findViewById(R.id.btn_decline);
        btnAccept = findViewById(R.id.btn_accept);

        layoutTalkingPanel = findViewById(R.id.layout_talking_panel);
        btnMuteMic = findViewById(R.id.btn_mute_mic);
        btnHangup = findViewById(R.id.btn_hangup);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);

        tvMicStatus = findViewById(R.id.tv_mic_status);
        tvHangupLabel = findViewById(R.id.tv_hangup_label);

        // 点击事件绑定
        btnAccept.setOnClickListener(v -> handleAccept());
        btnDecline.setOnClickListener(v -> handleReject());

        // 🌟 取消 / 挂断同一个按钮根据状态分发
        btnHangup.setOnClickListener(v -> {
            if (currentState == STATE_CALLING) {
                handleCancel(); // 未接通前取消
            } else {
                handleHangup(); // 通话中挂断
            }
        });

        btnMuteMic.setOnClickListener(v -> toggleMicrophone());
        btnSwitchCamera.setOnClickListener(v -> {
            if (mRtcEngine != null) {
                mRtcEngine.switchCamera(); // 微信功能：前后摄像头切换
            }
        });
    }

    /**
     * 🌟 初始化呼叫状态机
     */
    private void initCallState() {
        if (isCaller) {
            updateUIByState(STATE_CALLING);
            String channelId = getChannelId();
            RtcSignalingManager.sendVideoInvite(friendId, channelId);
            initAgoraAndPrepareLocalVideo();
        } else {
            updateUIByState(STATE_RINGING);
            startRingingAndVibration();
        }
    }

    /**
     * 🌟 状态机控制 UI 切换
     */
    private void updateUIByState(int state) {
        this.currentState = state;
        switch (state) {
            case STATE_CALLING: // 1. 主叫等待接听中
                layoutRingingPanel.setVisibility(View.GONE);
                layoutTalkingPanel.setVisibility(View.VISIBLE);

                btnMuteMic.setVisibility(View.INVISIBLE);
                btnSwitchCamera.setVisibility(View.INVISIBLE);

                tvCallStatus.setText("等待对方接受邀请...");
                tvHangupLabel.setText("取消");
                break;

            case STATE_RINGING: // 2. 被叫收到呼叫响铃中
                layoutRingingPanel.setVisibility(View.VISIBLE);
                layoutTalkingPanel.setVisibility(View.GONE);

                tvCallStatus.setText("邀请你进行视频通话...");
                break;

            case STATE_TALKING: // 3. 双方视频通话中
                layoutRingingPanel.setVisibility(View.GONE);
                layoutTalkingPanel.setVisibility(View.VISIBLE);

                btnMuteMic.setVisibility(View.VISIBLE);
                btnSwitchCamera.setVisibility(View.VISIBLE);

                tvHangupLabel.setText("挂断");
                break;
        }
    }

    /**
     * 🌟 被叫方点击【接听】视频
     */
    private void handleAccept() {
        stopRingingAndVibration();
        updateUIByState(STATE_TALKING);
        tvCallStatus.setText("正在连接视频...");

        updateMicButtonUI();

        // 2. 发送【1202 接受视频通话】信令
        RtcSignalingManager.acceptVideoCall(friendId);

        // 初始化声网并加入房间
        initAgoraAndJoin();
    }

    /**
     * 🌟 被叫方点击【拒绝】
     */
    private void handleReject() {
        stopRingingAndVibration();
        RtcSignalingManager.rejectVideoCall(friendId);
        finish();
    }

    /**
     * 🌟 主叫方未接通前点击【取消】
     */
    private void handleCancel() {
        stopRingingAndVibration();
        RtcSignalingManager.cancelVideoCall(friendId);
        leaveChannel();
        finish();
    }

    /**
     * 🌟 通话中途点击【挂断】
     */
    private void handleHangup() {
        stopRingingAndVibration();
        stopCallTimer();

        RtcSignalingManager.hangupVideoCall(friendId);
        leaveChannel();
        finish();
    }

    /**
     * 🌟 注册本地广播，监听 HomeActivity 分发的远程信令
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerRtcReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_VIDEO_CALL_ACCEPTED"); // 对方接听了
        filter.addAction("ACTION_VIDEO_CALL_REJECTED"); // 对方拒绝了
        filter.addAction("ACTION_VIDEO_CALL_CANCELED"); // 对方取消了
        filter.addAction("ACTION_VIDEO_CALL_HANGUP");   // 对方挂断了
        filter.addAction("ACTION_VIDEO_CALL_BUSY");     // 对方占线中

        registerReceiver(rtcReceiver, filter);
    }

    private final BroadcastReceiver rtcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if ("ACTION_VIDEO_CALL_ACCEPTED".equals(action)) {
                // 主叫方收到：对方接听了视频！
                updateUIByState(STATE_TALKING);
                tvCallStatus.setText("正在连接视频...");
                initAgoraAndJoin();

            } else if ("ACTION_VIDEO_CALL_REJECTED".equals(action)) {
                Toast.makeText(VideoCallActivity.this, "对方拒绝了视频通话", Toast.LENGTH_SHORT).show();
                finish();

            } else if ("ACTION_VIDEO_CALL_CANCELED".equals(action) || "ACTION_VIDEO_CALL_HANGUP".equals(action)) {
                stopRingingAndVibration();
                stopCallTimer();
                leaveChannel();
                finish();

            } else if ("ACTION_VIDEO_CALL_BUSY".equals(action)) {
                Toast.makeText(VideoCallActivity.this, "对方正忙，请稍后再试", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };

    // ==================== 声网 RTC 视频引擎逻辑 ====================

    private void initAgoraAndPrepareLocalVideo() {
        try {
            ensureEngineCreated();
            mRtcEngine.enableVideo();
            mRtcEngine.enableAudio();
            setupLocalVideo();
        } catch (Exception e) {
            Log.e("VideoCallActivity", "初始化声网视频引擎失败", e);
        }
    }

    private void initAgoraAndJoin() {
        try {
            ensureEngineCreated();

            mRtcEngine.enableVideo();
            mRtcEngine.enableAudio();
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            mRtcEngine.setEnableSpeakerphone(true); // 视频通话默认强制开免提

            setupLocalVideo();

            String channelName = getChannelId();
            long myId = SessionManager.getUserId();
            Log.d(TAG, "正在加入视频频道: " + channelName);
            mRtcEngine.joinChannel(null, channelName, "", (int) myId);

        } catch (Exception e) {
            Log.e(TAG, "初始化视频引擎失败", e);
        }
    }

    private void ensureEngineCreated() throws Exception {
        if (mRtcEngine == null) {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = AGORA_APP_ID;
            config.mEventHandler = mRtcEventHandler;

            mRtcEngine = RtcEngine.create(config);
        }
    }

    /**
     * 🌟 渲染本地前置摄像头 (右上角小悬浮窗口)
     */
    private void setupLocalVideo() {
        if (mRtcEngine == null) return;
        containerLocalVideo.removeAllViews();

        // lite-sdk 移除了 CreateRendererView, 直接实例化 SurfaceView (原方法内部即此实现)
        SurfaceView localSurfaceView = new SurfaceView(this);
        localSurfaceView.setZOrderMediaOverlay(true); // 🌟 关键：设置悬浮在远端大视频上方！
        containerLocalVideo.addView(localSurfaceView);

        mRtcEngine.setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        mRtcEngine.startPreview(); // 开启本地预览
    }

    /**
     * 🌟 渲染对方的远端视频 (全屏背景大窗口)
     */
    private void setupRemoteVideo(int uid) {
        if (mRtcEngine == null) return;
        containerRemoteVideo.removeAllViews();

        SurfaceView remoteSurfaceView = new SurfaceView(this);
        containerRemoteVideo.addView(remoteSurfaceView);

        mRtcEngine.setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> Log.d(TAG, "成功加入视频频道: " + channel));
        }

        // 🌟 核心：对方接通进入房间！渲染对方视频大画面并启动计时器！
        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> {
                Toast.makeText(VideoCallActivity.this, "视频通话已接通", Toast.LENGTH_SHORT).show();

                // 接通后隐藏头像占位, 展示对方视频画面
                ivCallAvatar.setVisibility(View.GONE);

                // 渲染对方全屏视频
                setupRemoteVideo(uid);

                // 启动通话计时器 (微信同款 00:01)
                startCallTimer();
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                Toast.makeText(VideoCallActivity.this, "通话已结束", Toast.LENGTH_SHORT).show();
                handleHangup();
            });
        }
    };

    // ==================== 辅助工具方法 ====================

    private String getChannelId() {
        long myId = SessionManager.getUserId();
        long minId = Math.min(myId, friendId);
        long maxId = Math.max(myId, friendId);
        return "room_" + minId + "_" + maxId;
    }

    private void startCallTimer() {
        callDurationSeconds = 0;
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                callDurationSeconds++;
                int minutes = callDurationSeconds / 60;
                int seconds = callDurationSeconds % 60;

                String timeStr = String.format(Locale.CHINA, "通话中 %02d:%02d", minutes, seconds);
                tvCallStatus.setText(timeStr);

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopCallTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void toggleMicrophone() {
        if (mRtcEngine == null) return;
        isMicMuted = !isMicMuted;
        mRtcEngine.muteLocalAudioStream(isMicMuted);
        updateMicButtonUI();
    }

    private void updateMicButtonUI() {
        if (!isMicMuted) {
            btnMuteMic.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnMuteMic.setIconTint(ColorStateList.valueOf(Color.parseColor("#1B1B1D")));
            tvMicStatus.setText("麦克风已开");
        } else {
            btnMuteMic.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")));
            btnMuteMic.setIconTint(ColorStateList.valueOf(Color.WHITE));
            tvMicStatus.setText("麦克风已关");
        }
    }

    private void leaveChannel() {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
            mRtcEngine = null;
        }
    }

    private void startRingingAndVibration() {
        try {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
            if (ringtone != null) ringtone.play();

            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(new long[]{1000, 1000, 1000, 1000}, 0);
            }
        } catch (Exception e) {
            Log.e("VideoCallActivity", "播放铃声或震动失败", e);
        }
    }

    private void stopRingingAndVibration() {
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
        if (vibrator != null) vibrator.cancel();
    }

    private void requestPermissions() {
        String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[1]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 101);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingingAndVibration();
        stopCallTimer();
        leaveChannel();
        try {
            unregisterReceiver(rtcReceiver); // 销毁广播防内存泄漏
        } catch (Exception ignored) {}
    }
}