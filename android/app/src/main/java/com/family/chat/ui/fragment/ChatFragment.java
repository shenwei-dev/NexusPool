package com.family.chat.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.family.chat.R;
import com.family.chat.ui.activity.ChatActivity;
import com.family.chat.ui.activity.HomeActivity;
import com.family.chat.ui.adapter.ChatSessionAdapter;
import com.family.chat.common.ApiResponse;
import com.family.chat.vo.ChatSessionVO;
import com.family.chat.repository.ChatSessionRepository;

import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView rvChatSessions;
    private ChatSessionAdapter adapter;

     private ChatSessionRepository chatSessionRepository;

     public ChatFragment() {
         chatSessionRepository = new ChatSessionRepository();
     }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initListener();
    }

    private void initView(View view) {
        rvChatSessions = view.findViewById(R.id.rv_chat_sessions);
        rvChatSessions.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ChatSessionAdapter();
        rvChatSessions.setAdapter(adapter);
    }

    /**
     * 【生命周期一：Tab 切换刷新】
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadChatSessions(); // 切回微信首屏时刷新
        }
    }

    /**
     * 【生命周期二：返回刷新】
     */
    @Override
    public void onResume() {
        super.onResume();
        loadChatSessions(); // 从聊天窗口退出返回首屏时刷新
    }

    /**
     * 异步拉取最近消息会话列表 (Notify and Pull)
     */
    public void loadChatSessions() {
        chatSessionRepository.getRecentSessions(new ChatSessionRepository.ChatSessionCallback<ApiResponse<List<ChatSessionVO>>>() {
            @Override
            public void onSuccess(ApiResponse<List<ChatSessionVO>> response) {
                List<ChatSessionVO> sessions = response.getData();
                adapter.setNewData(sessions);
                int totalUnreadCount = 0;
                for (ChatSessionVO session : sessions) {
                    if (session.getUnreadCount() != null) {
                        totalUnreadCount += session.getUnreadCount();
                    }
                }
                HomeActivity homeActivity = (HomeActivity) getActivity();
                if (homeActivity != null) {
                    homeActivity.updateChatBadge(totalUnreadCount);
                }
            }
            @Override
            public void onFailure(String error) {
                Log.e("ChatFragment", "会话列表拉取失败: " + error);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initListener() {
        adapter.setOnItemClickListener(session -> {
            HomeActivity homeActivity = (HomeActivity) getActivity();
            if (homeActivity != null && session.getUnreadCount() != null && session.getUnreadCount() > 0) {
                TextView tvTabChatBadge = homeActivity.findViewById(R.id.tv_tab_chat_badge);
                if (tvTabChatBadge != null && tvTabChatBadge.getVisibility() == View.VISIBLE) {
                    int currentTotalUnread = 0;
                    String currentText = tvTabChatBadge.getText().toString();
                    if (!android.text.TextUtils.isEmpty(currentText)) {
                        currentTotalUnread = Integer.parseInt(currentText);
                    }
                    int clickedSessionUnread = session.getUnreadCount();
                    int remainingTotalUnread = currentTotalUnread - clickedSessionUnread;
                    if (remainingTotalUnread > 0) {
                        tvTabChatBadge.setText(String.valueOf(remainingTotalUnread));
                    } else {
                        tvTabChatBadge.setVisibility(View.GONE);
                        tvTabChatBadge.setText("");
                    }
                }
            }

            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("friend_id", session.getFriendId()); // 传好友ID
            intent.putExtra("nickname", session.getDisplayName()); // 传备注/昵称
            startActivity(intent);
        });

        adapter.setOnItemLongClickListener((session, view) -> {
            showSessionOptionsDialog(session);
        });
    }

    /**
     * 弹出微信同款会话管理菜单（精细控制置顶/隐藏/删除）
     */
    private void showSessionOptionsDialog(ChatSessionVO session) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_session_options, null);

        TextView tvOptionPin = dialogView.findViewById(R.id.tv_option_pin);
        TextView tvOptionHide = dialogView.findViewById(R.id.tv_option_hide);
        TextView tvOptionDelete = dialogView.findViewById(R.id.tv_option_delete);

        // A. 【微信高级细节】：根据当前会话是否已经置顶，动态改变第一行的文字
        boolean isAlreadyPinned = session.getPinned() != null && session.getPinned() == 1;
        tvOptionPin.setText(isAlreadyPinned ? "取消置顶" : "置顶该聊天");

        // B. 处理 [置顶 / 取消置顶]
        tvOptionPin.setOnClickListener(v -> {
            dialog.dismiss();
            int targetPinState = isAlreadyPinned ? 0 : 1; // 取反

            chatSessionRepository.togglePin(session.getFriendId(), targetPinState, new ChatSessionRepository.ChatSessionCallback<ApiResponse<Void>>() {
                @Override
                public void onSuccess(ApiResponse<Void> response) {
                    if (response.getCode() == 1000) {
                        Toast.makeText(getContext(), isAlreadyPinned ? "已取消置顶" : "已置顶", Toast.LENGTH_SHORT).show();
                        loadChatSessions();
                    }
                }
                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // C. 处理 [不显示该聊天]（只删首屏列表，不删历史记录，本地 Room 消息明细不碰）
        tvOptionHide.setOnClickListener(v -> {
            dialog.dismiss();

            chatSessionRepository.hideSession(session.getFriendId(), new ChatSessionRepository.ChatSessionCallback<ApiResponse<Void>>() {
                @Override
                public void onSuccess(ApiResponse<Void> response) {
                    if (response.getCode() == 1000) {
                        loadChatSessions();
                    }
                }
                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // D. 处理 [删除该聊天]（删除首屏列表 + 物理清空本地 Room 缓存记录 + 物理清空云端 MySQL 记录）
        tvOptionDelete.setOnClickListener(v -> {
            dialog.dismiss();

            // 1. 【物理删除】：先清空当前手机本地的 Room 数据库历史记录缓存，保证离线可用性彻底清空
            com.family.chat.db.MessageDao messageDao =
                    com.family.chat.db.AppDatabase.getInstance(requireContext()).messageDao();

            // 假定您在 MessageDao 中有根据好友ID清空历史的方法（如：@Query("DELETE FROM local_message WHERE friend_id = :friendId")）
            messageDao.deleteHistoryByFriendId(session.getFriendId());

            // 2. 调用后端接口：删除首屏 + 物理清空云端 MySQL 记录明细
            chatSessionRepository.deleteSessionAndHistory(session.getFriendId(), new ChatSessionRepository.ChatSessionCallback<ApiResponse<Void>>() {
                @Override
                public void onSuccess(ApiResponse<Void> response) {
                    if (response.getCode() == 1000) {
                        Toast.makeText(getContext(), "已删除该聊天并清空记录", Toast.LENGTH_SHORT).show();
                        loadChatSessions(); // 刷新首屏，列表完全重置
                    }
                }
                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.setContentView(dialogView);
        // 让 dialog 背后半透明
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }
}