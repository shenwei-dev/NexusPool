package com.family.chat.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.family.chat.ChatApplication;
import com.family.chat.R;
import com.family.chat.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

public class AddFriendProfileActivity extends AppCompatActivity {

    private LinearLayout btnBackContainer;
    private ShapeableImageView ivAvatar;
    private TextView tvName;
    private MaterialButton btnAddToContacts;

    private long strangerAccount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend_profile);

        // 接收从搜索页传过来的目标用户信息
        strangerAccount = getIntent().getLongExtra("account", -1L);
        String nickname = getIntent().getStringExtra("nickname");
        String avatarUrl = getIntent().getStringExtra("avatarUrl");

        initView();
        initData(nickname, avatarUrl);
        initListener();
    }

    private void initView() {
        btnBackContainer = findViewById(R.id.btn_back_container);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvName = findViewById(R.id.tv_name);
        btnAddToContacts = findViewById(R.id.btn_add_to_contacts);
    }

    private void initData(String nickname, String avatar) {
        tvName.setText(nickname);

        // 提示：此处可以使用 Glide 加载陌生人的头像
        // Glide.with(this).load(avatar).placeholder(R.drawable.default_avatar).into(ivAvatar);
    }

    private void initListener() {
        // 返回

        btnBackContainer.setOnClickListener(v -> finish());

        btnAddToContacts.setOnClickListener(v -> {
            Intent intent = new Intent(AddFriendProfileActivity.this, SendFriendRequestActivity.class);
            String nickname = SessionManager.getNickname();
            intent.putExtra("account", strangerAccount);
            intent.putExtra("nickname", nickname);
            startActivity(intent);
            finish();
        });
    }
}
