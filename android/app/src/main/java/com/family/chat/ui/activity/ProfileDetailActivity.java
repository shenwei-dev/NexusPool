package com.family.chat.ui.activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.family.chat.ChatApplication;
import com.family.chat.R;
import com.family.chat.config.FilePathConfig;
import com.family.chat.repository.UserRepository;
import com.family.chat.util.AppExecutors;
import com.family.chat.util.ChatImageUtil;
import com.family.chat.util.SessionManager;
import com.family.chat.util.UriUtil;
import com.google.android.material.imageview.ShapeableImageView;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.view.UCropView;

import java.io.File;
import java.lang.reflect.Method;

public class ProfileDetailActivity extends AppCompatActivity {

    private LinearLayout btnBackContainer;
    private ConstraintLayout layoutChangeAvatar;
    private ConstraintLayout layoutChangeName;
    private ShapeableImageView ivDetailAvatar; // 头像控件
    private TextView tvDetailName;

    private UserRepository userRepository;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_detail);

        userRepository = new UserRepository();
        initView();
        initData();
        initAvatarCropLaunchers();
        initListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData(); // 重新刷新昵称和头像
    }

    private void initView() {
        btnBackContainer = findViewById(R.id.btn_back_container);
        layoutChangeAvatar = findViewById(R.id.layout_change_avatar);
        layoutChangeName = findViewById(R.id.layout_change_name);
        ivDetailAvatar = findViewById(R.id.iv_detail_avatar);
        tvDetailName = findViewById(R.id.tv_detail_name);
    }

    private void initData() {
        String nickname = SessionManager.getNickname();
        tvDetailName.setText(nickname);
        renderAvatar();
    }

    private void initListener() {
        btnBackContainer.setOnClickListener(v -> finish());
        layoutChangeAvatar.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        layoutChangeName.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileDetailActivity.this, ChangeNameActivity.class);
            startActivity(intent);
        });
    }


    private void startUCropActivity(Uri sourceUri) {
        File destinationFile = new File(getCacheDir(), "cropped_avatar_" + System.currentTimeMillis() + ".jpg");
        Uri destinationUri = Uri.fromFile(destinationFile);
        getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                // 当 UCropActivity 页面渲染展示时触发
                if (activity instanceof com.yalantis.ucrop.UCropActivity) {

                    // 1. 🌟 手写【取消】按钮点击事件 ➔ 强制关闭退出！
                    View btnCancel = activity.findViewById(R.id.ucrop_button_cancel);
                    if (btnCancel != null) {
                        btnCancel.setOnClickListener(v -> {
                            Log.d("UCropDebug", "点击了手写【取消】按钮，退出页面");
                            activity.setResult(Activity.RESULT_CANCELED);
                            activity.finish(); // 强行关闭退出，返回上一页！
                        });
                    }

                    // 2. 🌟 手写【确定】按钮点击事件 ➔ 强制触发 UCrop 官方裁剪保存！
                    View btnCrop = activity.findViewById(R.id.ucrop_button_crop);
                    if (btnCrop != null) {
                        btnCrop.setOnClickListener(v -> {
                            Log.d("UCropDebug", "点击了手写【确定】按钮，开始触发裁剪");
                            try {
                                // 利用 Java 反射，强行调用 UCropActivity 内部的 cropAndSaveImage() 裁剪保存方法！
                                Method cropMethod = activity.getClass().getDeclaredMethod("cropAndSaveImage");
                                cropMethod.setAccessible(true);
                                cropMethod.invoke(activity); // 100% 触发官方裁剪与结果返回！
                            } catch (Exception e) {
                                Log.e("UCropDebug", "反射触发 UCrop 裁剪失败", e);
                            }
                        });
                    }
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity instanceof com.yalantis.ucrop.UCropActivity) {
                    Window window = activity.getWindow();
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setNavigationBarColor(Color.parseColor("#0D0E11"));
                    WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
                    if (controller != null) {
                        controller.hide(WindowInsetsCompat.Type.statusBars());
                        controller.setAppearanceLightNavigationBars(false);
                    }
                    window.setStatusBarColor(Color.parseColor("#1A1A1A"));
                }
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (activity instanceof com.yalantis.ucrop.UCropActivity) {
                    getApplication().unregisterActivityLifecycleCallbacks(this);
                }
            }

            @Override public void onActivityStarted(@NonNull Activity activity) {}
            @Override public void onActivityPaused(@NonNull Activity activity) {}
            @Override public void onActivityStopped(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
        });


        UCrop.Options options = new UCrop.Options();
        // 1. 彻底隐藏底部轮盘/旋转/缩放控件
        options.setHideBottomControls(true);        // 🌟【核心新增 1】：设置整个页面 (包含图片上方、四周、下方所有空白处) 根背景色为 #1A1A1A
        options.setRootViewBackgroundColor(Color.parseColor("#1A1A1A"));

        // 2. 暗色遮罩与微信绿边框
//        options.setDimmedLayerColor(Color.parseColor("#DD000000")); // 深黑半透明遮罩
        options.setDimmedLayerColor(Color.parseColor("#FF1A1A1A"));
        options.setCropFrameColor(Color.parseColor("#07C160"));   // 微信绿边框
        options.setCropGridColor(Color.parseColor("#22FFFFFF"));  // 淡白网格

        // 3. 强制锁定 1:1 正方形
        options.setFreeStyleCropEnabled(false);
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);

        Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)       // 1:1 比例
                .withMaxResultSize(800, 800)  // 800x800 高清分辨率
                .withOptions(options)
                .getIntent(this);

        cropLauncher.launch(cropIntent);
    }

    /**
     * 🌟 初始化相册选择与 UCrop 正方形裁剪 Launchers
     */
    private void initAvatarCropLaunchers() {

        // 1. 🌟 裁剪结果 Launcher：处理用户点击【确定】或【取消】后的逻辑
        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri croppedUri = UCrop.getOutput(result.getData());
                        if (croppedUri != null) {
                            String localCroppedPath = UriUtil.getFilePathFromUri(this, croppedUri);
                            if (TextUtils.isEmpty(localCroppedPath)) {
                                Toast.makeText(this, "图片路径读取失败", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Glide.with(this)
                                    .load(new File(localCroppedPath))
                                    .placeholder(R.color.grey_light)
                                    .into(ivDetailAvatar);
                            Toast.makeText(this, "正在上传新头像...", Toast.LENGTH_SHORT).show();
                            userRepository.updateAvatar(localCroppedPath, new UserRepository.UpdateAvatarCallback() {
                                @Override
                                public void onSuccess(String newAvatarUrl) {

                                    if (!isFinishing() && !isDestroyed()) {
                                        Toast.makeText(ProfileDetailActivity.this, "头像修改成功！", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(String errorMsg) {
                                    if (!isFinishing() && !isDestroyed()) {
                                        Toast.makeText(ProfileDetailActivity.this, "修改失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                                        android.util.Log.e("ProfileDetailActivity", "失败原因" + errorMsg);
                                    }
                                }
                            });
                        }

                        // ------------------ 情况 B：用户点击了【取消】退出裁剪 ------------------
                    } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        Toast.makeText(this, "已取消修改头像", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 2. 🌟 相册选择器 Launcher：选完图片后，自动调起 UCrop 正方形裁剪框
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        // 选图成功，拉起 UCrop 裁剪页面！
                        startUCropActivity(uri);
                    }
                }
        );
    }

    /**
     * 🌟 大厂三级降级头像渲染逻辑
     */
    private void renderAvatar() {
        if (isFinishing() || isDestroyed()) return;

        Long userId = SessionManager.getUserId();
        File fixedAvatarFile = FilePathConfig.getFixedAvatarFile(userId);
        if (fixedAvatarFile.exists() && fixedAvatarFile.length() > 0) {
            Log.d("ProfileDetailActivity", "🎯 命中固定路径本地头像，0 毫秒加载: " + fixedAvatarFile.getAbsolutePath());
            Glide.with(this)
                    .load(fixedAvatarFile) // 👈 加载固定的本地文件！
                    // 固定路径文件会被新头像覆盖, 附加修改时间签名防止 Glide 命中旧图缓存
                    .signature(new ObjectKey(fixedAvatarFile.lastModified()))
                    .placeholder(R.color.grey_light)
                    .into(ivDetailAvatar);
        } else {
            String ossUrl = SessionManager.getOssAvatarUrl();
            if (!TextUtils.isEmpty(ossUrl)) {
                Glide.with(this)
                        .load(ossUrl)
                        .placeholder(R.color.grey_light)
                        .into(ivDetailAvatar);
                asyncDownloadToFixedPath(userId, ossUrl);
            }
        }
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
                    Log.d("ProfileDetailActivity", "🎉 OSS 头像已成功落盘到固定路径！");
                }
            } catch (Exception e) {
                Log.e("ProfileDetailActivity", "异步下载 OSS 头像至固定路径失败", e);
            }
        });
    }
}