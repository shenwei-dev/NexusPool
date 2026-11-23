package com.family.chat.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

import com.family.chat.R;
import com.family.chat.viewmodel.SearchContactViewModel;

public class SearchContactActivity extends AppCompatActivity {

    private SearchContactViewModel viewModel;

    private EditText etSearchInput;
    private ImageView btnClear;
    private TextView btnCancel;

    private ConstraintLayout layoutSearchEntry;
    private TextView tvSearchQueryText;
    private TextView tvNotFound;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_contact);

        initView();
        initListener();
        initViewModel(); // 👈 核心初始化：建立 VM 观察链路
        autoShowKeyboard();
    }

    private void initView() {
        etSearchInput = findViewById(R.id.et_search_input);
        btnClear = findViewById(R.id.btn_clear);
        btnCancel = findViewById(R.id.btn_cancel);

        layoutSearchEntry = findViewById(R.id.layout_search_entry);
        tvSearchQueryText = findViewById(R.id.tv_search_query_text);
        tvNotFound = findViewById(R.id.tv_not_found);
    }

    private void initViewModel() {
        // 1. 实例化我们的 SearchContactViewModel
        viewModel = new ViewModelProvider(this).get(SearchContactViewModel.class);

        // 2. 观察搜索结果数据变化
        viewModel.getSearchResult().observe(this, user -> {
            if (user != null) {
                // 情况一：搜到了用户，隐藏错误文本，携带数据跳转至极简添加好友页
                tvNotFound.setVisibility(View.GONE);

                Intent intent = new Intent(SearchContactActivity.this, AddFriendProfileActivity.class);
                intent.putExtra("account", user.getAccount());
                intent.putExtra("nickname", user.getNickname());
                intent.putExtra("avatarUrl", user.getAvatarUrl());
                startActivity(intent);
            } else {
                // 情况二：没搜到用户 (user == null)
                showUserNotFoundState();
            }
        });

        // 3. 观察通用错误提示
        viewModel.getErrorMessage().observe(this, error -> {
            if (!TextUtils.isEmpty(error)) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // 4. 观察加载状态 (此处可根据需要控制 ProgressBar 的显示/隐藏)
        viewModel.getIsLoading().observe(this, isLoading -> {
            // if (isLoading) showLoading() else hideLoading()
        });
    }

    private void initListener() {
        btnCancel.setOnClickListener(v -> finish());

        btnClear.setOnClickListener(v -> {
            etSearchInput.setText("");
            etSearchInput.requestFocus();
            showSoftKeyboard();
        });

        // 文本输入状态变化监听
        etSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                boolean hasQuery = !TextUtils.isEmpty(query);

                if (hasQuery) {
                    btnClear.setVisibility(View.VISIBLE);
                    layoutSearchEntry.setVisibility(View.VISIBLE);
                    tvNotFound.setVisibility(View.GONE); // 输入变动时，立刻隐藏历史“不存在”提示

                    // 关键词高亮变绿
                    String styledText = "搜索:<font color='#07C160'>" + query + "</font>";
                    tvSearchQueryText.setText(Html.fromHtml(styledText));
                } else {
                    btnClear.setVisibility(View.GONE);
                    layoutSearchEntry.setVisibility(View.GONE);
                    tvNotFound.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 点击“搜索:xxx”这一行时，委托给 ViewModel 进行异步请求
        layoutSearchEntry.setOnClickListener(v -> {
            String query = etSearchInput.getText().toString().trim();
            if (!TextUtils.isEmpty(query)) {
                // 👈 核心连接：调用 VM 里的搜索方法
                viewModel.searchUser(query);
            }
        });
    }

    /**
     * 没搜到用户时，自适应收起键盘并展示提示
     */
    private void showUserNotFoundState() {
        tvNotFound.setVisibility(View.VISIBLE);
        layoutSearchEntry.setVisibility(View.GONE);

        // 收起键盘
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearchInput.getWindowToken(), 0);
        }
    }

    private void autoShowKeyboard() {
        etSearchInput.requestFocus();
        etSearchInput.postDelayed(this::showSoftKeyboard, 100);
    }

    private void showSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etSearchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }
}