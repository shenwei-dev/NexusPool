package com.family.chat.ui.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.family.chat.ChatApplication;
import com.family.chat.R;
import com.family.chat.util.SessionManager;
import com.family.chat.viewmodel.SendFriendRequestViewModel;
import com.google.android.material.button.MaterialButton;

public class SendFriendRequestActivity extends AppCompatActivity {

    private SendFriendRequestViewModel viewModel;
    private LinearLayout btnBackContainer;
    private EditText etGreetingInput;
    private EditText etRemarkInput;
    private MaterialButton btnSend;
    private long targetAccount;
    private String remark;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_friend_request);

        targetAccount = getIntent().getLongExtra("account", -1L);
        remark = getIntent().getStringExtra("nickname");

        if (targetAccount == -1L) {
            Toast.makeText(this, "异常：未找到目标账号", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initView();
        initData();
        initViewModel();
        initListener();
    }

    private void initView() {
        btnBackContainer = findViewById(R.id.btn_back_container);
        etGreetingInput = findViewById(R.id.et_greeting_input);
        etRemarkInput = findViewById(R.id.et_remark_input);
        btnSend = findViewById(R.id.btn_send);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(SendFriendRequestViewModel.class);

        // 监听发送状态
        viewModel.getIsSendSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "发送成功，等待验证", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (!TextUtils.isEmpty(error)) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                btnSend.setEnabled(false);
                btnSend.setText("正在发送...");
            } else {
                btnSend.setEnabled(true);
                btnSend.setText("发送");
            }
        });
    }

    private void initData() {
        String myNickname = SessionManager.getNickname();

        if (!TextUtils.isEmpty(myNickname)) {
            String defaultGreeting = "我是 " + myNickname;
            etGreetingInput.setText(defaultGreeting);
            etGreetingInput.setSelection(defaultGreeting.length());
        }
    }

    private void initListener() {
        btnBackContainer.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String greeting = etGreetingInput.getText().toString().trim();
            String remark = etRemarkInput.getText().toString().trim();

            if (remark.equals("")) {
                remark = this.remark;
            }
            // 👈 核心连接：交给 ViewModel 执行请求
            viewModel.sendFriendRequest(targetAccount, greeting, remark);
        });
    }
}