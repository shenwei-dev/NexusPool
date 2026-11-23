package com.family.chat.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.family.chat.ChatApplication;
import com.family.chat.R;
import com.family.chat.api.RetrofitClient;
import com.family.chat.api.UserApi;
import com.family.chat.common.ApiResponse;
import com.family.chat.config.FilePathConfig;
import com.family.chat.network.WebSocketManager;
import com.family.chat.ui.activity.ChatActivity;
import com.family.chat.ui.activity.LoginActivity;
import com.family.chat.ui.activity.ProfileDetailActivity;
import com.family.chat.util.AppExecutors;
import com.family.chat.util.ChatImageUtil;
import com.family.chat.util.SessionManager;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeFragment extends Fragment {

    private static final String TAG = "MeFragment";

    private ConstraintLayout layoutProfileHeader;
    private ShapeableImageView ivProfileAvatar;
    private TextView tvProfileNickname;
    private TextView tvProfileWechatId;
    private final UserApi userApi;
    private ConstraintLayout layoutAiAssistant;
    private ConstraintLayout layoutSettings;

    public MeFragment() {
        this.userApi = RetrofitClient.getInstance().create(UserApi.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_me, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);
        initData();
        initListener();
    }

    // 🌟 核心：从个人资料页 (ProfileDetailActivity) 换完头像返回时，自动重新刷出新头像！
    @Override
    public void onResume() {
        super.onResume();
        initData();
        setupWhiteStatusBar();
    }

    private void initView(View view) {
        layoutProfileHeader = view.findViewById(R.id.layout_profile_header);
        ivProfileAvatar = view.findViewById(R.id.iv_profile_avatar);
        tvProfileNickname = view.findViewById(R.id.tv_profile_nickname);
        tvProfileWechatId = view.findViewById(R.id.tv_profile_wechat_id);

        layoutAiAssistant = view.findViewById(R.id.layout_ai_assistant);
        layoutSettings = view.findViewById(R.id.layout_settings);
    }

    /**
     * 自动从本地缓存加载我自己的昵称、账号与头像
     */
    private void initData() {
        if (!isAdded() || getContext() == null) return;
        long account = SessionManager.getAccount();
        String nickname = SessionManager.getNickname();
        if (!TextUtils.isEmpty(nickname)) {
            tvProfileNickname.setText(nickname);
        }

        if (!TextUtils.isEmpty(Long.toString(account))) {
            tvProfileWechatId.setText("微信号: " + account);
        }
        renderAvatar();
    }

    /**
     * 🌟 大厂三级降级头像渲染逻辑
     */
    private void renderAvatar() {
        if (!isAdded() || getContext() == null) return;
        Long userId = SessionManager.getUserId();
        File fixedAvatarFile = FilePathConfig.getFixedAvatarFile(userId);
        if (fixedAvatarFile.exists() && fixedAvatarFile.length() > 0) {
            Log.d(TAG, "🎯 命中固定路径本地头像，0 毫秒加载: " + fixedAvatarFile.getAbsolutePath());
            Glide.with(this)
                    .load(fixedAvatarFile) // 👈 加载固定的本地文件！
                    // 固定路径文件会被新头像覆盖, 附加修改时间签名防止 Glide 命中旧图缓存
                    .signature(new ObjectKey(fixedAvatarFile.lastModified()))
                    .placeholder(R.color.grey_light)
                    .into(ivProfileAvatar);
        } else {
            String ossUrl = SessionManager.getOssAvatarUrl();
            if (!TextUtils.isEmpty(ossUrl)) {
                Glide.with(this)
                        .load(ossUrl)
                        .placeholder(R.color.grey_light)
                        .into(ivProfileAvatar);
                asyncDownloadToFixedPath(userId, ossUrl);
            }
        }
    }

    /**
     * 异步后台将 OSS 头像图片下载落盘，并更新 SessionManager 本地路径
     */
    private void asyncDownloadAndSaveAvatar(String ossUrl) {
        AppExecutors.io().execute(() -> {
            try {
                File downloadedFile = Glide.with(ChatApplication.getContext())
                        .asFile()
                        .load(ossUrl)
                        .submit()
                        .get();
                if (downloadedFile != null && downloadedFile.exists()) {
                    String savedSandboxPath = ChatImageUtil.saveAvatarToAppDir(downloadedFile.getAbsolutePath());
                    SessionManager.saveLocalAvatarPath(savedSandboxPath);
                    Log.d(TAG, "🎉 OSS 头像已成功下载并缓存至本地沙盒: " + savedSandboxPath);
                }
            } catch (Exception e) {
                Log.e(TAG, "异步下载保存 OSS 头像失败", e);
            }
        });
    }

    /**
     * 异步下载 OSS 头像存入固定路径
     */
    private void asyncDownloadToFixedPath(Long userId, String ossUrl) {
        AppExecutors.io().execute(() -> {
            try {
                File downloadedFile = Glide.with(ChatApplication.getContext())
                        .asFile()
                        .load(ossUrl)
                        .submit()
                        .get();

                if (downloadedFile != null && downloadedFile.exists()) {
                    ChatImageUtil.saveUserAvatarFixedPath(userId, downloadedFile.getAbsolutePath());
                    Log.d(TAG, "🎉 OSS 头像已成功落盘到固定路径！");
                }
            } catch (Exception e) {
                Log.e(TAG, "异步下载 OSS 头像至固定路径失败", e);
            }
        });
    }

    private void initListener() {
        layoutProfileHeader.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProfileDetailActivity.class);
            startActivity(intent);
        });

        // 智能助手点击事件
        layoutAiAssistant.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("friend_id", 99999L);
            intent.putExtra("nickname", "智能助手");
            startActivity(intent);
        });

        // 设置/退出登录点击事件
        layoutSettings.setOnClickListener(v -> handleLogout());
    }

    private void handleLogout() {
        userApi.logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                executeLocalLogout();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                executeLocalLogout();
            }
        });
    }

    /**
     * 客户端本地退登清理（带 Context 安全防护，防止 requireContext 崩溃）
     */
    private void executeLocalLogout() {
        WebSocketManager.getInstance().disconnect();
        SessionManager.clearSession();
        if (isAdded() && getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void setupWhiteStatusBar() {
        if (getActivity() == null) return;

        Window window = getActivity().getWindow();

        // 1. 清除半透明，允许绘制状态栏背景
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // 2. 🌟 设置状态栏背景颜色为纯白 #FFFFFF
        window.setStatusBarColor(Color.parseColor("#FFFFFF"));

        Log.d(TAG, "🎉 状态栏背景颜色: " + window.getStatusBarColor());
        // 3. 🌟【最关键】：把状态栏上面的系统文字/图标（时间、电池、网络）变成深色/黑色！
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true); // true 代表黑色图标/文字
        }
    }
}