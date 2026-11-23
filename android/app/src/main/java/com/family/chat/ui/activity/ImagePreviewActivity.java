package com.family.chat.ui.activity;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.family.chat.R;

import java.io.File;

/**
 * 微信同款：极致沉浸式全屏图片查看器 (隐藏顶部状态栏与底部导航栏)
 */
public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🌟【核心 1】：在 setContentView 之前强行开启全屏沉浸模式！
        hideSystemBars();

        setContentView(R.layout.activity_image_preview);

        PhotoView photoView = findViewById(R.id.photo_view);
        View btnClose = findViewById(R.id.btn_close);

        String imageUrl = getIntent().getStringExtra("image_url");

        if (TextUtils.isEmpty(imageUrl)) {
            Toast.makeText(this, "图片路径无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 智能判定：如果是本地物理路径包装成 File，网络 URL 传 String
        Object loadTarget;
        if (imageUrl != null && !imageUrl.startsWith("http")) {
            loadTarget = new File(imageUrl);
        } else {
            loadTarget = imageUrl;
        }

        // Glide 渲染到 PhotoView
        Glide.with(this)
                .load(loadTarget)
                .placeholder(R.color.grey_light)
                .into(photoView);

        // 单击图片退出全屏
        photoView.setOnViewTapListener((view, x, y) -> finish());

        // 点击左上角关闭按钮退出
        btnClose.setOnClickListener(v -> finish());
    }

    /**
     * 🌟【核心 2】：隐藏顶部状态栏(时间/电量) 与 底部导航栏(虚拟按键/返回键)
     */
    private void hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        if (insetsController != null) {
            // 1. 隐藏顶部状态栏 + 底部导航栏
            insetsController.hide(WindowInsetsCompat.Type.systemBars());

            // 2. 设置为滑动边缘时才短暂浮现系统栏 (微信同款 IMMERSIVE_STICKY 效果)
            insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        // 3. 允许内容延伸到刘海屏/挖孔屏区域，做到 100% 全屏铺满！
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
    }

    /**
     * 🌟【核心 3】：窗口重新获得焦点时再次隐藏 (防止下拉通知栏后系统栏重新弹出来)
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }
}