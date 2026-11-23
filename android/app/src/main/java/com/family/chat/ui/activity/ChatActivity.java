package com.family.chat.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.family.chat.R;
import com.family.chat.ui.adapter.ChatAdapter;
import com.family.chat.repository.ChatSessionRepository;
import com.family.chat.viewmodel.ChatViewModel;
import android.content.ClipData;
import android.content.ClipboardManager;

import java.util.List;


public class ChatActivity extends AppCompatActivity {

    private LinearLayout btnChatBack;
    private TextView tvChatTitle;
    private ImageView btnChatMore;
    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private AppCompatButton btnSend;
    private Long friendId;
    private String nickname;
    private String remark;
    private ChatViewModel viewModel;
    private ChatAdapter chatAdapter;
    private ImageView btnPlus;
    private LinearLayout layoutMorePanel;
    private LinearLayout btnOptionAlbum;
    private ActivityResultLauncher<Intent> mPhotoPickerLauncher;

    public static Long activeChatFriendId = -1L;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!parseIntentData()) {
            return;
        }
        initLaunchers();
        setContentView(R.layout.activity_chat);
        setupSystemBar();
        initView();
        initToolbar();
        initViewModel();
        initInputListener();
        new ChatSessionRepository().clearUnreadCount(friendId);
    }

    private void initView() {
        btnChatBack = findViewById(R.id.btn_chat_back);
        btnPlus = findViewById(R.id.btn_plus);
        tvChatTitle = findViewById(R.id.tv_chat_title);
        btnChatMore = findViewById(R.id.btn_chat_more);
        rvMessages = findViewById(R.id.rv_messages);
        etMessageInput = findViewById(R.id.et_message_input);
        btnSend = findViewById(R.id.btn_send);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter();
        rvMessages.setAdapter(chatAdapter);
        layoutMorePanel = findViewById(R.id.layout_more_panel);
        btnOptionAlbum = findViewById(R.id.btn_option_album);
        btnChatBack.setOnClickListener(v -> finish());
        chatAdapter.setOnBubbleLongClickListener((message, view) -> {
            showMessageOptionsDialog(message);
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(false);
        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                rvMessages.post(() -> {
                    if (chatAdapter != null && chatAdapter.getItemCount() > 0) {
                        rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                });
            }
        });
        RecyclerView.ItemAnimator animator = rvMessages.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        rvMessages.setLayoutManager(layoutManager);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.initChat(friendId);

        viewModel.getMessageList(friendId).observe(this, messages -> {
            Log.d("ChatActivity", "UI LiveData 被触发！当前消息数量 = " + (messages != null ? messages.size() : 0));
            if (messages != null) {
                if (messages.isEmpty() && chatAdapter.getItemCount() > 0) {
                    Log.w("StatusDebug", "⚠️ 忽略 Room 的临时 0 数据推送，防止清空列表！");
                    return;
                }
                chatAdapter.setNewData(messages);
                int newSize = messages.size();
                if (newSize > 0) {
                    rvMessages.post(() -> {
                        LinearLayoutManager layoutManager = (LinearLayoutManager) rvMessages.getLayoutManager();
                        if (layoutManager != null) {
                            layoutManager.scrollToPositionWithOffset(newSize - 1, 0);
                        }
                    });
                }
            }
        });
        viewModel.getErrorMessage().observe(this, error -> {
            if (!TextUtils.isEmpty(error)) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getAckEvent().observe(this, tempId -> {
            if (tempId != null && chatAdapter != null) {
                chatAdapter.notifyItemChangedByTempId(tempId);
            }
        });
    }

    private void initToolbar() {
        String displayName;
        if (!TextUtils.isEmpty(remark)) {
            displayName = remark;
        } else if (!TextUtils.isEmpty(nickname)) {
            displayName = nickname;
        } else {
            displayName = "未命名好友";
        }
        tvChatTitle.setText(displayName);
        btnChatMore.setOnClickListener(v -> {
        });
    }

    private void initInputListener() {
        etMessageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasContent = s != null && s.toString().trim().length() > 0;
                if (hasContent) {
                    btnPlus.setVisibility(View.GONE);
                    btnSend.setVisibility(View.VISIBLE);
                    btnSend.setEnabled(true);
                    btnSend.setTextColor(Color.WHITE);
                } else {
                    btnPlus.setVisibility(View.VISIBLE);
                    btnSend.setVisibility(View.GONE);
                    btnSend.setEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        etMessageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                rvMessages.postDelayed(() -> {
                    if (chatAdapter != null && chatAdapter.getItemCount() > 0) {
                        rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                }, 200);
            }
        });
        btnSend.setOnClickListener(v -> {
            sendTextMessage();
        });
        // 5. 👈 【体验核心】：点击输入框（键盘准备弹起）时，必须【立刻强制隐藏】多媒体面板，防止视图冲突！
        etMessageInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                layoutMorePanel.setVisibility(View.GONE); // 强制隐藏面板
            }
            return false;
        });

        btnPlus.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null && imm.isActive(etMessageInput)) {
                layoutMorePanel.setVisibility(View.VISIBLE);
                imm.hideSoftInputFromWindow(etMessageInput.getWindowToken(), 0);
                etMessageInput.clearFocus();
                scrollChatListToBottom();
            } else {
                if (layoutMorePanel.getVisibility() == View.VISIBLE) {
                    layoutMorePanel.setVisibility(View.GONE);
                } else {
                    layoutMorePanel.setVisibility(View.VISIBLE);
                    scrollChatListToBottom();
                }
            }
        });

        // 7. 👈 【点击相册】：通过启动器一行代码安全唤起系统图库！
        btnOptionAlbum.setOnClickListener(v -> {
            layoutMorePanel.setVisibility(View.GONE); // 退出面板
            Intent intent = new Intent(ChatActivity.this, PhotoPickerActivity.class);
            mPhotoPickerLauncher.launch(intent);
        });
    }

    private void sendTextMessage() {
        String content = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            return;
        }
        viewModel.sendTextMessage(content);
        etMessageInput.setText("");
        rvMessages.postDelayed(() -> {
            if (chatAdapter.getItemCount() > 0) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) rvMessages.getLayoutManager();
                if (layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(chatAdapter.getItemCount() - 1, 0);
                }
            }
        }, 50);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeChatFriendId = friendId;
    }

    @Override
    protected void onPause() {
        super.onPause();
        activeChatFriendId = -1L;
        new ChatSessionRepository().clearUnreadCount(friendId);
    }

    private void showMessageOptionsDialog(com.family.chat.model.ChatMessage message) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_message_options, null);

        TextView tvOptionCopy = dialogView.findViewById(R.id.tv_option_copy);
        TextView tvOptionDelete = dialogView.findViewById(R.id.tv_option_delete);

        tvOptionCopy.setOnClickListener(v -> {
            dialog.dismiss();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("chat_msg", message.getContent());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
            }
        });

        tvOptionDelete.setOnClickListener(v -> {
            dialog.dismiss();
            Long messageId = message.getTempId();
            viewModel.deleteLocalMessage(messageId);
        });
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void scrollChatListToBottom() {
        rvMessages.postDelayed(() -> {
            if (chatAdapter != null && chatAdapter.getItemCount() > 0) {
                rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
            }
        }, 100);
    }

    /**
     * 解析并校验 Intent 传参 (Fail-Fast 机制)
     */
    private boolean parseIntentData() {
        friendId = getIntent().getLongExtra("friend_id", -1L);
        nickname = getIntent().getStringExtra("nickname");
        remark = getIntent().getStringExtra("remark");
        if (friendId == -1L) {
            Toast.makeText(this, "异常：未指定聊天对象", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        return true;
    }

    /**
     * 统一初始化 Launchers 回调
     */
    private void initLaunchers() {
        initPhotoPickerLauncher();
    }

    /**
     * 初始化自研相册选择器 Launcher
     */
    private void initPhotoPickerLauncher() {
        mPhotoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        List<String> paths = (List<String>) result.getData().getSerializableExtra("selected_photos");
                        if (paths != null && !paths.isEmpty()) {
                            for (String localPath : paths) {
                                Log.d("ChatDebug", "准备上传并发送本地图片: " + localPath);
                                viewModel.sendImageMessage(localPath);
                            }
                        }
                    }
                });
    }

    /**
     * 优化沉浸式状态栏/导航栏设置 (告别繁琐的 if 版本判断，采用现代 Jetpack WindowCompat API)
     */
    private void setupSystemBar() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#F3F3F3"));
        window.setNavigationBarColor(Color.parseColor("#F7F7F7"));
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
        }
    }
}