package com.family.chat.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.family.chat.R;
import com.family.chat.viewmodel.FriendSettingsViewModel;

public class FriendSettingsActivity extends AppCompatActivity {

    private FriendSettingsViewModel viewModel;

    private Toolbar toolbar;
    private SwitchCompat switchBlacklist;
    private FrameLayout btnDeleteFriend;

    private Long friendId;
    private boolean isUserAction = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_settings);

        friendId = getIntent().getLongExtra("friend_id", -1L);
        boolean initialBlackStatus = getIntent().getBooleanExtra("is_blocked", false);

        if (friendId == -1L) {
            Toast.makeText(this, "无效的参数", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initView();
        initViewModel();

        // 设置初始开关状态
        setSwitchStateWithoutTriggering(initialBlackStatus);

        initListener();
    }

    /** 初始化View */
    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        switchBlacklist = findViewById(R.id.switch_blacklist);
        btnDeleteFriend = findViewById(R.id.btn_delete_friend);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /** 初始化监听器 */
    private void initListener() {
        // 监听开关状态变更
        switchBlacklist.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUserAction) {
                Toast.makeText(this, "已经拉黑", Toast.LENGTH_SHORT).show();
            }
        });


        // 监听删除按钮点击，弹出微信同款警告对话框
        btnDeleteFriend.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    /** 初始化ViewModel */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(FriendSettingsViewModel.class);

        // 监听拉黑状态变化以同步开关
        viewModel.getBlacklistStatus().observe(this, this::setSwitchStateWithoutTriggering);

        // 监听 Toast 提醒
        viewModel.getToastMessage().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // 监听删除成功信号
        viewModel.getIsDeleteSuccess().observe(this, success -> {
            Log.d("DeleteFriend", "Activity 中的 getIsDeleteSuccess 观察者被触发，收到 success = " + success);
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();

                // 1. 构建跳转至 HomeActivity 的 Intent
                Intent intent = new Intent(this, HomeActivity.class);

                // 2. 传递标记：指定跳转目标为好友列表页
                intent.putExtra("target_tab", "friends_tab");

                // 3. 使用 CLEAR_TOP 确保清理掉详情页和设置页
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 修改 Switch 状态，同时屏蔽 OnCheckedChangeListener 避免循环调用
     */
    private void setSwitchStateWithoutTriggering(boolean checked) {
        isUserAction = false;
        switchBlacklist.setChecked(checked);
        isUserAction = true;
    }

    /**
     * 弹出二次确认删除的 Dialog
     */
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("将联系人删除")
                .setMessage("将同时删除与该联系人的聊天记录")
                .setPositiveButton("删除", (dialog, which) -> viewModel.deleteFriend(friendId))
                .setNegativeButton("取消", null)
                .create()
                .show();
    }
}
