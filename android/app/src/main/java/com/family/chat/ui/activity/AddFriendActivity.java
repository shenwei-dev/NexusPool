package com.family.chat.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.family.chat.R;
import com.family.chat.ui.adapter.NewFriendsAdapter;
import com.family.chat.vo.QueryFriendRequestListVO;
import com.family.chat.viewmodel.AddFriendViewModel;
import java.util.List;

public class AddFriendActivity extends AppCompatActivity {

    private AddFriendViewModel viewModel;

    private Toolbar toolbar;
    private EditText etSearchFriend;
    private RecyclerView rvFriendRequests;

    private NewFriendsAdapter adapter;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        initView();
        initListener();
        initViewModel();
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar_add_friend);
        etSearchFriend = findViewById(R.id.et_search_friend);
        rvFriendRequests = findViewById(R.id.rv_friend_requests);

        // 设置导航返回箭头监听
        toolbar.setNavigationOnClickListener(v -> finish());

        // 初始化 RecyclerView 及其适配器
        rvFriendRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NewFriendsAdapter();
        rvFriendRequests.setAdapter(adapter);
    }

    /**
     * 【体验优化】：每次从二级页面返回时，都自动触发列表刷新
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadFriendRequests(); // 刷新好友申请列表
        }
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(AddFriendViewModel.class);

        // 1. 监听好友请求列表并刷新渲染
        viewModel.getFriendRequests().observe(this, list -> {
            if (list != null) {
                adapter.setNewData(list);
            }
        });

        // 2. 监听同意接受申请成功的通知
        viewModel.getAcceptSuccessPosition().observe(this, position -> {
            if (position != null) {
                Toast.makeText(this, "已添加该好友", Toast.LENGTH_SHORT).show();

                // 获取当前内存中的数据列表
                List<QueryFriendRequestListVO> currentList = viewModel.getFriendRequests().getValue();
                if (currentList != null && position < currentList.size()) {

                    // 1. 物理改变该行数据的状态为：1（已同意）
                    currentList.get(position).setStatus(1);

                    // 2. 【大厂级性能优化】：仅重绘发生改变的这一行，其余行绝不产生闪烁，体验极佳！
                    adapter.notifyItemChanged(position);
                }
            }
        });

        // 3. 监听报错
        viewModel.getErrorMessage().observe(this, error -> {
            if (!TextUtils.isEmpty(error)) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initListener() {
        etSearchFriend.setOnClickListener(v -> gotoSearchPage());
        adapter.setOnAcceptClickListener((id, position) -> {
            viewModel.acceptFriendRequest(id, position);
        });
    }

    private void gotoSearchPage() {
        Intent intent = new Intent(this, SearchContactActivity.class);
        startActivity(intent);
    }
}