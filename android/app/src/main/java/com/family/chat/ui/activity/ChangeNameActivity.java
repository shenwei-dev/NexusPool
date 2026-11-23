package com.family.chat.ui.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.family.chat.R;
import com.family.chat.repository.UserRepository;
import com.family.chat.util.SessionManager;
import com.google.android.material.button.MaterialButton;

public class ChangeNameActivity extends AppCompatActivity {

    private LinearLayout btnBackContainer;
    private MaterialButton btnSave;
    private EditText etNicknameInput;
    private boolean hasBeenModified = false;
    private String originalName; // 记录进入页面时的原始昵称
    private ProgressDialog loadingDialog; // 加载弹窗
    private UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_name);

        originalName = SessionManager.getNickname();
        initView();
        initData();
        initListener();
    }

    private void initView() {
        btnBackContainer = findViewById(R.id.btn_back_container);
        btnSave = findViewById(R.id.btn_save);
        etNicknameInput = findViewById(R.id.et_name_input);
    }

    private void initData() {
        // 填充原昵称，并将光标移动至文字末尾
        etNicknameInput.setText(originalName);
        etNicknameInput.setSelection(originalName.length());

        // 👈 【体验核心】：界面加载后，立即让输入框聚焦并自动弹出键盘
        etNicknameInput.requestFocus();
        etNicknameInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etNicknameInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100); // 延迟100ms等待Window生命周期稳定后弹出
    }

    private void initListener() {
        // 返回
        btnBackContainer.setOnClickListener(v -> finish());

        // 监听输入框文本变化
        etNicknameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String currentInput = s.toString().trim();

                // 1. 【核心逻辑】：只要当前输入内容与原昵称不同，就将“已被修改”标记永久设为 true
                if (!currentInput.equals(originalName)) {
                    hasBeenModified = true;
                }

                // 2. 按钮启用判定：必须【被修改过】且【当前输入框不为空】
                boolean isEnabled = hasBeenModified && !TextUtils.isEmpty(currentInput);

                btnSave.setEnabled(isEnabled);

                if (isEnabled) {
                    // 状态：激活（微信绿）
                    btnSave.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#07C160")));
                    btnSave.setTextColor(Color.WHITE);
                } else {
                    // 状态：禁用（微信灰底）
                    btnSave.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E1E1E1")));
                    btnSave.setTextColor(Color.parseColor("#B2B2B2"));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 保存点击事件
        btnSave.setOnClickListener(v -> {
            handleSaveNickname();
        });
    }

    private void handleSaveNickname() {
        String newNickname = etNicknameInput.getText().toString().trim();
        if (TextUtils.isEmpty(newNickname)) {
            Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newNickname.length() > 16) {
            Toast.makeText(this, "昵称不能超过 16 个字符", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading("正在保存...");
        userRepository.updateNickname(newNickname, new UserRepository.UpdateNicknameCallback() {
            @Override
            public void onSuccess(String updatedNickname) {
                dismissLoading();
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(ChangeNameActivity.this, "昵称修改成功", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(String errorMsg) {
                dismissLoading();
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(ChangeNameActivity.this, "修改失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLoading(String msg) {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setCanceledOnTouchOutside(false); // 阻止点击空白处取消
        }
        loadingDialog.setMessage(msg);
        loadingDialog.show();
    }

    private void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.dismiss(); // 安全关闭
            } catch (Exception e) {
                Log.w("ChangeNameActivity", "关闭加载弹窗失败: " + e.getMessage());
            }
        }
    }
}