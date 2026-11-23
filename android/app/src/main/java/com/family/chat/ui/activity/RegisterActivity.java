package com.family.chat.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.family.chat.R;
import com.family.chat.viewmodel.RegisterViewModel;
import com.google.android.material.button.MaterialButton;

public class RegisterActivity extends AppCompatActivity {

    /** 密码输入框 */
    private EditText etPasswordReg;

    /** 密码输入框的眼睛 */
    private ImageView btnEye;

    /** 注册按钮 */
    private MaterialButton btnRegister;

    /** 登录页跳转超链接 */
    private TextView tvGotoLogin;

    /** 记录密码是否可见 */
    private boolean isPasswordVisible = false;

    /** 视图模型层 */
    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initView();
        initViewModel();
        initListener();
    }

    private void initView() {
        etPasswordReg = findViewById(R.id.et_password_reg);
        btnEye = findViewById(R.id.btn_eye);
        btnRegister = findViewById(R.id.btn_register);
        tvGotoLogin = findViewById(R.id.tv_goto_login);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        viewModel.getRegistrationResult().observe(this, response -> {
            if (response != null && response.getCode() == 1000 && response.getData() != null) {

                Long allocatedAccount = response.getData().getAccount();

                // 弹窗展示这个账号，并支持一键复制
                showRegisterSuccessDialog(allocatedAccount);
            }
        });

        // 2. 监听报错（包含我们刚刚写的：密码必须为8-16位字母数字组合校验失败提示）
        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (!TextUtils.isEmpty(errorMsg)) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });

        // 监听加载状态
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                btnRegister.setEnabled(false);
                btnRegister.setText("正在提交注册...");
            } else {
                btnRegister.setEnabled(true);
                btnRegister.setText("注册并获取账号");
            }
        });
    }

    private void initListener() {
        // 跳转到登录页面
        tvGotoLogin.setOnClickListener(v -> finish());

        btnEye.setOnClickListener(v -> {
            // 切换小眼睛状态
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPasswordReg.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnEye.setImageResource(R.drawable.ic_eye_open);
            } else {
                etPasswordReg.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnEye.setImageResource(R.drawable.ic_eye_close);
            }
            etPasswordReg.setSelection(etPasswordReg.getText().length());
        });

        btnRegister.setOnClickListener(v -> {
            String password = etPasswordReg.getText().toString().trim();
            viewModel.register(password);
        });
    }

    /**
     * 弹出高保真注册成功分配账号 Dialog
     */
    private void showRegisterSuccessDialog(Long account) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        new AlertDialog.Builder(this)
                .setTitle("🎉 注册成功！")
                .setMessage("系统已自动为您分配账号：\n\n" + account + "\n\n请妥善保管您的登录账号。")
                .setCancelable(false) // 强制不允许点击外面销毁，必须点击按钮
                .setPositiveButton("复制并去登录", (dialogInterface, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("allocated_account", String.valueOf(account));
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "账号已复制到剪贴板", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                })
                .create()
                .show();
    }
}





