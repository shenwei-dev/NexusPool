package com.family.chat.ui.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.family.chat.R;
import com.family.chat.network.RtcSignalingManager;
import com.family.chat.network.WebSocketManager;
import com.family.chat.util.AvatarManager;
import com.family.chat.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import java.util.Locale;

public class VoiceCallActivity extends AppCompatActivity {

    /** 等待对方接听 */
    private static final int STATE_CALLING = 1;

    /** 收到来电响铃 */
    private static final int STATE_RINGING = 2;

    /** 通话中 */
    private static final int STATE_TALKING = 3;

    private int currentState = STATE_CALLING;

    /** 标签 */
    private static final String TAG = "VoiceCallActivity";

    /** 声网 AppID, 由 local.properties 注入 */
    private static final String AGORA_APP_ID = com.family.chat.BuildConfig.AGORA_APP_ID;

    /** 通话状态 */
    private TextView tvCallStatus;

    /** 通话好友昵称 */
    private TextView tvCallName;

    /** 通话好友头像 */
    private ImageView ivCallAvatar;

    private ImageView ivMinimize;

    /** 来电面板 */
    private LinearLayout layoutRingingPanel;

    /** 拒绝按钮 */
    private MaterialButton btnDecline;

    /** 接听按钮 */
    private MaterialButton btnAccept;

    /** 通话中面板 */
    private LinearLayout layoutTalkingPanel;

    /** 麦克风按钮 */
    private MaterialButton btnMuteMic;

    /** 挂断按钮 */
    private MaterialButton btnHangup;

    /** 扬声器按钮 */
    private MaterialButton btnToggleSpeaker;

    /** 麦克风状态描述 */
    private TextView tvMicStatus;

    /** 扬声器状态描述 */
    private TextView tvSpeakerStatus;

    /** 挂断按钮的文本描述 */
    private TextView tvHangupLabel;

    /** 当前好友ID */
    private Long friendId;

    /** 是否是通话发起者 */
    private boolean isCaller;

    /** 通话引擎 */
    private RtcEngine mRtcEngine;

    /** 默认开启麦克风 */
    private boolean isMicMuted = false;

    /** 默认使用听筒 */
    private boolean isSpeakerOn = false;

    private Vibrator vibrator;

    private Ringtone ringtone;

    /** 通话持续秒数 */
    private int callDurationSeconds = 0;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    private Runnable timerRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);
        friendId = getIntent().getLongExtra("friend_id", -1L);
        isCaller = getIntent().getBooleanExtra("is_caller", false);
        String nickname = getIntent().getStringExtra("nickname");
        requestMicrophonePermission();
        initView();
        AvatarManager.load(ivCallAvatar, friendId); // 加载对方真实头像
        initListener();
        initToolbar(nickname);
        initCallState();
        registerRtcReceiver();
    }

    private void initView() {
        tvCallStatus = findViewById(R.id.tv_call_status);
        tvCallName = findViewById(R.id.tv_call_name);
        ivCallAvatar = findViewById(R.id.iv_call_avatar);
        ivMinimize = findViewById(R.id.iv_minimize);
        layoutRingingPanel = findViewById(R.id.layout_ringing_panel);
        btnDecline = findViewById(R.id.btn_decline);
        btnAccept = findViewById(R.id.btn_accept);
        layoutTalkingPanel = findViewById(R.id.layout_talking_panel);
        btnMuteMic = findViewById(R.id.btn_mute_mic);
        btnHangup = findViewById(R.id.btn_hangup);
        btnToggleSpeaker = findViewById(R.id.btn_toggle_speaker);
        tvMicStatus = findViewById(R.id.tv_mic_status);
        tvSpeakerStatus = findViewById(R.id.tv_speaker_status);
        tvHangupLabel = findViewById(R.id.tv_hangup_label);
    }

    private void initListener() {
        btnDecline.setOnClickListener(v -> handleReject());
        btnAccept.setOnClickListener(v -> handleAccept());
        btnHangup.setOnClickListener(v -> {
            if (currentState == STATE_CALLING) {
                handleCancel();
            } else if (currentState == STATE_TALKING) {
                handleHangup();
            }
        });
        ivMinimize.setOnClickListener(v -> finish());
        btnMuteMic.setOnClickListener(v -> toggleMicrophone());
        btnToggleSpeaker.setOnClickListener(v -> toggleSpeakerphone());
    }

    private void initToolbar(String nickname) {
        if (nickname != null) {
            tvCallName.setText(nickname);
        } else {
            tvCallName.setText("好友名称获取异常");
        }
    }

    private void initCallState() {
        if (isCaller) {
            layoutRingingPanel.setVisibility(View.GONE);
            layoutTalkingPanel.setVisibility(View.VISIBLE);
            tvCallStatus.setText("等待对方接受邀请...");
            tvHangupLabel.setText("取消");
            updateMicButtonUI();
            updateSpeakerButtonUI();
            String channelId = getChannelId();
            RtcSignalingManager.sendAudioInvite(friendId, channelId);
        } else {
            layoutRingingPanel.setVisibility(View.VISIBLE);
            layoutTalkingPanel.setVisibility(View.GONE);
            tvCallStatus.setText("邀请你进行语音通话...");
            startRingingAndVibration();
        }
    }

    /**
     * 控制麦克风静音切换
     */
    private void toggleMicrophone() {
        if (mRtcEngine == null) return;
        isMicMuted = !isMicMuted;
        mRtcEngine.muteLocalAudioStream(isMicMuted);
        updateMicButtonUI();
    }

    /**
     * 更新麦克风按钮样式
     */
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

    /**
     * 控制扬声器免提切换
     */
    private void toggleSpeakerphone() {
        if (mRtcEngine == null) return;
        isSpeakerOn = !isSpeakerOn;
        mRtcEngine.setEnableSpeakerphone(isSpeakerOn);
        updateSpeakerButtonUI();
    }

    /**
     * 更新扬声器按钮样式
     */
    private void updateSpeakerButtonUI() {
        if (isSpeakerOn) {
            btnToggleSpeaker.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnToggleSpeaker.setIconTint(ColorStateList.valueOf(Color.parseColor("#1B1B1D")));
            tvSpeakerStatus.setText("扬声器已开");
        } else {
            btnToggleSpeaker.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")));
            btnToggleSpeaker.setIconTint(ColorStateList.valueOf(Color.WHITE));
            tvSpeakerStatus.setText("扬声器已关");
        }
    }

    /**
     * 接听对方发来的语音通话
     */
    private void handleAccept() {
        stopRingingAndVibration();
        layoutRingingPanel.setVisibility(View.GONE);
        layoutTalkingPanel.setVisibility(View.VISIBLE);
        tvCallStatus.setText("正在连接语音...");
        tvHangupLabel.setText("挂断");
        updateMicButtonUI();
        updateSpeakerButtonUI();
        RtcSignalingManager.acceptAudioCall(friendId);
        initAgoraAndJoin();
    }

    /**
     * 拒绝对方发来的语音通话
     */
    private void handleReject() {
        stopRingingAndVibration();
        RtcSignalingManager.rejectAudioCall(friendId);
        finish();
    }

    /**
     * 取消自己发起的语音通话(对方还未接听)
     */
    private void handleCancel() {
        RtcSignalingManager.cancelAudioCall(friendId);
        finish();
    }

    /**
     * 挂断语音通话(对方已经接听)
     */
    private void handleHangup() {
        RtcSignalingManager.hangupAudioCall(friendId);
        leaveChannel();
        finish();
    }

    /**
     * 播放铃声并开始震动
     */
    private void startRingingAndVibration() {
        try {
            // 获取系统默认铃声
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            // 创建铃声对象
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);

            // 如果铃声对象创建成功, 那么播放铃声
            if (ringtone != null) {
                ringtone.play();
            }

            // 获取震动服务
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            // 如果震动服务获取成功并且手机具有震动硬件
            if (vibrator != null && vibrator.hasVibrator()) {
                // 设置震动规律(停1秒,震动1秒)
                long[] pattern = {1000, 1000, 1000, 1000};

                // 开始循环震动
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception e) {
            Log.e("VoiceCallActivity", "播放铃声或震动失败", e);
        }
    }

    /**
     * 关闭响声和震动
     */
    private void stopRingingAndVibration() {
        // 如果铃声对象存在并且正在播放铃声
        if (ringtone != null && ringtone.isPlaying()) {
            // 关闭铃声
            ringtone.stop();
        }
        // 如果震动服务存在
        if (vibrator != null) {
            // 关闭震动
            vibrator.cancel();
        }
    }

    /**
     * 初始化声网 RTC 引擎并加入纯语音频道
     */
    private void initAgoraAndJoin() {
        try {
            if (mRtcEngine == null) {
                RtcEngineConfig config = new RtcEngineConfig();
                config.mContext = getApplicationContext();
                config.mAppId = AGORA_APP_ID;
                config.mEventHandler = mRtcEventHandler;
                mRtcEngine = RtcEngine.create(config);
            }
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            mRtcEngine.enableAudio();
            mRtcEngine.disableVideo();
            mRtcEngine.setEnableSpeakerphone(isSpeakerOn);
            long myId = SessionManager.getUserId();
            String channelName = getChannelId();
            Log.d(TAG, "正在加入声网纯语音频道: " + channelName + ", 本地 uid=" + myId);
            mRtcEngine.joinChannel(null, channelName, "", (int) myId);
        } catch (Exception e) {
            Log.e(TAG, "初始化声网引擎失败", e);
            Toast.makeText(this, "语音引擎初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 声网RTC引擎回调监听器
     */
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                Log.d(TAG, "本地成功加入频道: " + channel);
                if (mRtcEngine != null) {
                    mRtcEngine.setEnableSpeakerphone(isSpeakerOn); // 切换免提/听筒
                    mRtcEngine.adjustPlaybackSignalVolume(150);     // 适当放大对方音量
                    mRtcEngine.adjustRecordingSignalVolume(150);    // 适当放大麦克风录音音量
                }
            });
        }
        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> {
                Log.d(TAG, "对方接通并加入频道, uid: " + uid);
                stopRingingAndVibration(); // 停止响铃震动

                // 🌟 开启微信同款通话计时器 (00:01, 00:02...)
                startCallTimer();
            });
        }
        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                Log.d(TAG, "对方已挂断离线, 原因: " + reason);
                Toast.makeText(VoiceCallActivity.this, "对方已挂断，通话结束", Toast.LENGTH_SHORT).show();

                stopCallTimer(); // 停止计时
                handleHangup();  // 自动退出页面
            });
        }
        @Override
        public void onAudioRouteChanged(int routing) {
            runOnUiThread(() -> Log.d(TAG, "音频输出路由改变: " + routing));
        }
    };

    /**
     * 开启通话计时器
     */
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

    /**
     * 关闭通话计时器
     */
    private void stopCallTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void leaveChannel() {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
            mRtcEngine = null;
        }
    }

    private void requestMicrophonePermission() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, 101);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingingAndVibration();
        leaveChannel();
        WebSocketManager.getInstance().setChatListener(null);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerRtcReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_AUDIO_CALL_ACCEPTED"); // 对方接受接听
        filter.addAction("ACTION_AUDIO_CALL_REJECTED"); // 对方拒绝
        filter.addAction("ACTION_AUDIO_CALL_CANCELED"); // 对方取消
        filter.addAction("ACTION_AUDIO_CALL_HANGUP");   // 对方挂断
        filter.addAction("ACTION_AUDIO_CALL_BUSY");     // 对方占线

        registerReceiver(rtcReceiver, filter);
    }

    private final BroadcastReceiver rtcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if ("ACTION_AUDIO_CALL_ACCEPTED".equals(action)) {
                // 1. 主叫方收到：对方接听了！主叫方也加入声网频道并开始计时
                tvCallStatus.setText("通话中 00:00");
                tvHangupLabel.setText("挂断");
                initAgoraAndJoin();
            } else if ("ACTION_AUDIO_CALL_REJECTED".equals(action)) {
                // 2. 主叫方收到：对方拒绝了
                Toast.makeText(VoiceCallActivity.this, "对方拒绝了通话", Toast.LENGTH_SHORT).show();
                finish();
            } else if ("ACTION_AUDIO_CALL_CANCELED".equals(action) || "ACTION_AUDIO_CALL_HANGUP".equals(action)) {
                // 3. 收到对方取消/挂断
                stopRingingAndVibration();
                leaveChannel();
                finish();
            } else if ("ACTION_AUDIO_CALL_BUSY".equals(action)) {
                // 4. 收到对方占线
                Toast.makeText(VoiceCallActivity.this, "对方正忙，请稍后再试", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };

    /**
     * 获取语音频道ID
     */
    private String getChannelId() {
        long myId = SessionManager.getUserId();
        long minId = Math.min(myId, friendId);
        long maxId = Math.max(myId, friendId);
        return "room_" + minId + "_" + maxId;
    }

    private void updateUIByState(int state) {
        this.currentState = state;

        switch (state) {
            case STATE_CALLING: // 主叫等待接听中
                layoutRingingPanel.setVisibility(View.GONE);
                layoutTalkingPanel.setVisibility(View.VISIBLE);

                // 🌟 将按钮文字设为："取消"
                tvHangupLabel.setText("取消");
                break;

            case STATE_TALKING: // 双方通话中
                layoutRingingPanel.setVisibility(View.GONE);
                layoutTalkingPanel.setVisibility(View.VISIBLE);

                // 🌟 将按钮文字设为："挂断"
                tvHangupLabel.setText("挂断");
                break;
        }
    }
}