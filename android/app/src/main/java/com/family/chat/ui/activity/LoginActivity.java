package com.family.chat.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.family.chat.ChatApplication;
import com.family.chat.R;
import com.family.chat.util.SessionManager;
import com.family.chat.viewmodel.LoginViewModel;


public class LoginActivity extends AppCompatActivity {

    /** 用户名输入框 */
    private EditText usernameEdit;

    /** 密码输入框 */
    private EditText passwordEdit;

    /** 登录按钮，请求过程中会被禁用以防止重复点击 */
    private Button loginButton;

    /** 注册跳转链接 */
    private TextView registerLink;

    /**
     * 业务逻辑 ViewModel
     * <p>持有登录相关的 LiveData 状态。</p>
     */
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 调用父类 Activity 的 onCreate 方法
        super.onCreate(savedInstanceState);

        // 把 XML 布局文件 加载到当前 Activity 的屏幕上
        setContentView(R.layout.activity_login);

        // 初始化 UI 控件,初始化顺序：UI组件 -> ViewModel数据绑定 -> 事件监听
        initViews();
        initViewModel();
        initListeners();

        // 读取本地保存的 Token
        String token = SessionManager.getAccessToken();
        if (!TextUtils.isEmpty(token)) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            finish();
        }

    }

    /** 初始化View */
    private void initViews() {
        usernameEdit = findViewById(R.id.et_username);
        passwordEdit = findViewById(R.id.et_password);
        loginButton = findViewById(R.id.btn_login);
        registerLink = findViewById(R.id.tv_goto_register);
    }

    /** 初始化ViewModel */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        viewModel.getSuccessMessage().observe(this, message -> {
            showToast(message != null ? message : "登录成功");
            navigateToHome();
        });
        viewModel.getErrorMessage().observe(this, error -> {
            setLoadingState(false);
            showToast(error);
        });
    }

    /** 初始化监听器 */
    private void initListeners() {
        // 登录按钮点击事件
        loginButton.setOnClickListener(v -> {
            // 获取用户名
            Long account = Long.valueOf(usernameEdit.getText().toString().trim());

            // 获取密码
            String password = passwordEdit.getText().toString().trim();

            // 前置校验通过后，切换 UI 为 Loading 状态并由 ViewModel 发起请求
            if (isValidInput(account, password)) {
                setLoadingState(true);
                viewModel.login(account, password);
            }
        });

        // 注册链接点击事件
        registerLink.setOnClickListener(v -> navigateToRegister());
    }

    /** 跳转到首页 */
    private void navigateToHome() {
        // 创建Intent,从当前Activity跳转到MainActivity
        Intent intent = new Intent(this, HomeActivity.class);

        // 清空栈
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // 启动MainActivity
        startActivity(intent);
    }

    /** 跳转到注册页 */
    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    /** 设置登录状态 */
    private void setLoadingState(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        loginButton.setText(isLoading ? "登录中..." : "登录");
    }

    /** 校验输入合法性 */
    private boolean isValidInput(Long account, String password) {
        if (account.toString().isEmpty()) {
            // 输入框下面出现红色提示：请输入账号
            usernameEdit.setError("请输入账号");

            // 让输入框获得焦点
            usernameEdit.requestFocus();

            // 说明校验 失败，阻止继续后续操作。
            return false;
        }
        if (password.isEmpty()) {
            // 输入框下面出现红色提示：请输入密码
            passwordEdit.setError("请输入密码");

            // 让输入框获得焦点
            passwordEdit.requestFocus();

            // 说明校验 失败，阻止继续后续操作。
            return false;
        }
        return true;
    }

    private void showToast(String message) {
        // 在当前 Activity 上，创建一个内容为 message 的短时间 Toast，并显示出来。
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}