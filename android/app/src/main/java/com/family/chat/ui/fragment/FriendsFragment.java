package com.family.chat.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.family.chat.R;
import com.family.chat.ui.activity.AddFriendActivity;
import com.family.chat.ui.activity.FriendDetailActivity;
import com.family.chat.ui.activity.GroupListActivity;
import com.family.chat.ui.activity.HomeActivity;
import com.family.chat.ui.adapter.FriendsAdapter;
import com.family.chat.util.SessionManager;
import com.family.chat.viewmodel.FriendsViewModel;

public class FriendsFragment extends Fragment {

    /** 显示好友列表 */
    private RecyclerView rvFriends;

    /** "新的朋友"控件 */
    private LinearLayout layoutNewFriend;

//    /** "群聊"控件 */
//    private LinearLayout layoutGroupChat;

    private FriendsViewModel viewModel;

    private FriendsAdapter adapter;

    private TextView tvNewFriendUnreadCount;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewModel();
        initView(view);
        initListener();
        initRecyclerView();
        loadData();
    }

    private void initView(View view) {
        rvFriends = view.findViewById(R.id.rv_contacts);
        layoutNewFriend = view.findViewById(R.id.layout_new_friend);
//        layoutGroupChat = view.findViewById(R.id.layout_group_chat);
        tvNewFriendUnreadCount = view.findViewById(R.id.tv_new_friend_unread_count);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(FriendsViewModel.class);
        viewModel.getFriendList().observe(
                getViewLifecycleOwner(), friends -> {
                    adapter.setNewData(friends);
                });
        viewModel.getErrorMessage()
                .observe(getViewLifecycleOwner(), error ->
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show());
    }

    private void initListener() {
        layoutNewFriend.setOnClickListener(v -> {
            tvNewFriendUnreadCount.setVisibility(View.GONE);
            HomeActivity homeActivity = (HomeActivity) getActivity();
            if (homeActivity != null) {
                TextView tvTabContactsBadge = homeActivity.findViewById(R.id.tv_tab_contacts_badge);
                if (tvTabContactsBadge != null) {
                    tvTabContactsBadge.setVisibility(View.GONE);
                    tvTabContactsBadge.setText("");
                }
            }
            Intent intent = new Intent(getActivity(), AddFriendActivity.class);
            startActivity(intent);
        });

//        layoutGroupChat.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), GroupListActivity.class);
//            startActivity(intent);
//        });
    }

    private void initRecyclerView() {
        adapter = new FriendsAdapter();
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFriends.setAdapter(adapter);

        // 设置好友项的点击监听
        adapter.setOnItemClickListener((friend, position) -> {
            if (friend != null) {
                Intent intent = new Intent(getActivity(), FriendDetailActivity.class);

                // 将friend_id传递到好友详情页中
                intent.putExtra("friend_id", friend.getFriendId());

                // 跳转好友详情页
                startActivity(intent);
            }
        });
    }

    private void loadData() {
        if (viewModel != null) {
            viewModel.loadFriends();
        }
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadData();
            android.util.Log.d("FriendsFragment", "通讯录页面切换为显示状态，开始更新未读数字...");
            updateNewFriendUnreadCount();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            Log.d("FriendsFragment", "onResume：页面可见，重新请求好友列表数据");
            loadData();
        }
        updateNewFriendUnreadCount();
    }

    /**
     * 【抽取为公共方法】：从底栏 Badge 中获取真实的未读数，并渲染为纯红色字体的数字
     */
    /**
     * 【重构二】：直接、安全地从本地缓存中读取未读数字，100% 稳定防失效
     */
    private void updateNewFriendUnreadCount() {
        int count = SessionManager.getUnreadFriendRequestCount();
        if (count > 0) {
            tvNewFriendUnreadCount.setText(String.valueOf(count));
            tvNewFriendUnreadCount.setVisibility(View.VISIBLE);
        } else {
            tvNewFriendUnreadCount.setVisibility(View.GONE);
        }
    }
}

