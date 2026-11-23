package com.family.chat.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.family.chat.R;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import com.family.chat.util.AvatarManager;
import com.family.chat.vo.FriendDetailVO;
import com.family.chat.viewmodel.FriendDetailViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

public class FriendDetailActivity extends AppCompatActivity {

    private FriendDetailViewModel viewModel;

    // 1. 顶部导航栏控件
    private LinearLayout btnBackContainer;
    private ImageView btnMore;

    // 2. 核心信息卡片控件
    private ShapeableImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvName;
    private TextView tvUserId;

    // 4. 底部操作按钮
    private MaterialButton btnSendMessage;
    private MaterialButton btnAvCall;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_friend);

        long friendId = getIntent().getLongExtra("friend_id", -1L);


        initView();
        // 进页面立即从全局头像缓存渲染, 避免"默认头像->真实头像"闪变;
        // 网络/缓存数据到达后 renderDetail 会用最新 URL 刷新
        AvatarManager.load(ivAvatar, friendId);
        initListener(friendId);
        initViewModel();

        if (friendId != -1L) {
            viewModel.loadFriendDetail(friendId);
        } else {
            Toast.makeText(this, "未获取到有效的好友ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /** 初始化 XML 布局中的控件*/
    private void initView() {
        btnBackContainer = findViewById(R.id.btn_back_container);
        btnMore = findViewById(R.id.btn_more);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvName = findViewById(R.id.tv_name);
        tvNickname = findViewById(R.id.tv_nickname);
        tvUserId = findViewById(R.id.tv_user_id);
        btnSendMessage = findViewById(R.id.btn_send_message);
        btnAvCall = findViewById(R.id.btn_av_call);
    }

    private void initListener(Long friendId) {
        btnBackContainer.setOnClickListener(v -> finish());
        btnMore.setOnClickListener(v -> {
            FriendDetailVO detail = viewModel.getFriendDetail().getValue();
            if (detail != null) {
                Intent intent = new Intent(FriendDetailActivity.this, FriendSettingsActivity.class);
                intent.putExtra("friend_id", detail.getFriendId());
                startActivity(intent);
            }
        });
        btnSendMessage.setOnClickListener(v -> {
            FriendDetailVO detail = viewModel.getFriendDetail().getValue();
            if (detail != null) {
                Intent intent = new Intent(FriendDetailActivity.this, ChatActivity.class);
                intent.putExtra("friend_id", detail.getFriendId());
                intent.putExtra("nickname", detail.getNickname());
                intent.putExtra("remark", detail.getRemark());
                startActivity(intent);
            }
        });
        btnAvCall.setOnClickListener(v -> {
            showCallOptionDialog(friendId);
        });
    }

    /** 初始化 ViewModel 并设置 LiveData 监听 */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(FriendDetailViewModel.class);
        viewModel.getFriendDetail().observe(this, this::renderDetail);
        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (!TextUtils.isEmpty(errorMsg)) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                btnSendMessage.setEnabled(false);
                btnAvCall.setEnabled(false);
            } else {
                btnSendMessage.setEnabled(true);
                btnAvCall.setEnabled(true);
            }
        });
    }

    /**
     * 渲染查询到的好友详细数据到界面控件
     * @param detail 好友详细信息实体类
     */
    private void renderDetail(FriendDetailVO detail) {
        if (detail == null) {
            return;
        }
        String remark = detail.getRemark();
        String nickname = detail.getNickname();
        Long account = detail.getAccount();
        if (!TextUtils.isEmpty(remark)) {
            tvName.setText(remark);
            tvNickname.setText("昵称:  " + nickname);
            tvNickname.setVisibility(View.VISIBLE);
        } else {
            tvName.setText(nickname);
            tvNickname.setVisibility(View.GONE);
        }
        // 微信号在任何情况下都正常在最下方展示
        tvUserId.setText("微信号:  " + account);

        // 最新签名 URL 写入全局缓存后, 统一走本地固定路径文件缓存加载, 避免每次进详情页穿透网络
        AvatarManager.put(detail.getFriendId(), detail.getAvatarUrl());
        AvatarManager.load(ivAvatar, detail.getFriendId());
    }

    private void showCallOptionDialog(Long friendId) {
        // 1. 实例化 BottomSheetDialog 并挂载我们的透明去除黑角的主题
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);

        // 2. 加载我们刚刚写好的弹窗 XML 布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_call_options, null);

        TextView tvVideoCall = dialogView.findViewById(R.id.tv_video_call);
        TextView tvVoiceCall = dialogView.findViewById(R.id.tv_voice_call);
        TextView tvCancel = dialogView.findViewById(R.id.tv_cancel);

        // 3. 点击“视频通话”的逻辑
        tvVideoCall.setOnClickListener(v -> {
            dialog.dismiss(); // 销毁弹窗
            // 👈 核心连接：跳转到 VideoCallActivity 并标记我是拨打方
            Intent intent = new Intent(this, VideoCallActivity.class);
            intent.putExtra("friend_id", friendId);
            intent.putExtra("is_caller", true);
            startActivity(intent);
        });

        // 4. 点击“语音通话”的逻辑：携带数据直接跳转到我们的 VoiceCallActivity
        tvVoiceCall.setOnClickListener(v -> {
            dialog.dismiss(); // 销毁弹窗

            // 路由跳转
            Intent intent = new Intent(this, VoiceCallActivity.class);
            intent.putExtra("friend_id", friendId); // 传递当前被呼叫的好友 ID
            intent.putExtra("is_caller", true);     // 标记我是拨打方
            startActivity(intent);
        });

        // 5. 点击“取消”直接关闭弹窗
        tvCancel.setOnClickListener(v -> dialog.dismiss());

        // 6. 将视图装载入 Dialog 并展示
        dialog.setContentView(dialogView);
        dialog.show();
    }
}
