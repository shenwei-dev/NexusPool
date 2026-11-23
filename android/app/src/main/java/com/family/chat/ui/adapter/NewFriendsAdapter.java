package com.family.chat.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.family.chat.ChatApplication;
import com.family.chat.R;
import com.family.chat.util.SessionManager;
import com.family.chat.vo.QueryFriendRequestListVO;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class NewFriendsAdapter extends RecyclerView.Adapter<NewFriendsAdapter.RequestViewHolder> {

    private List<QueryFriendRequestListVO> dataList = new ArrayList<>();
    private OnAcceptClickListener acceptClickListener;

    // 定义同意按钮的点击事件接口
    public interface OnAcceptClickListener {
        void onAcceptClick(Long id, int position);
    }

    public void setOnAcceptClickListener(OnAcceptClickListener listener) {
        this.acceptClickListener = listener;
    }

    public void setNewData(List<QueryFriendRequestListVO> list) {
        this.dataList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 加载我们在前面章节中写好的微信标准的 item_friend_request 布局
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        QueryFriendRequestListVO item = dataList.get(position);
        if (item == null) return;
        Long myUserId = SessionManager.getUserId();

        // 设置昵称和打招呼内容
        holder.tvReqName.setText(item.getSenderNickname());
        String greeting = item.getGreeting();

        // ==================== 核心逻辑：微信同款状态多分支分流渲染 ====================

        if (item.getStatus() == 2) {
            holder.tvStatusAdded.setText("↗ 已过期");
        }

        if (item.getFromUserId() != null && item.getFromUserId().equals(myUserId)) {
            // 分支 A：是我主动申请加别人的
            holder.btnAccept.setVisibility(View.GONE);
            holder.tvStatusAdded.setVisibility(View.VISIBLE);
            holder.tvReqGreeting.setText("我: " + greeting);
            if (item.getStatus() == 0) {
                holder.tvStatusAdded.setText("↗ 等待验证"); // 对标截图：“↗ 等待验证”
            } else if (item.getStatus() == 1) {
                holder.tvStatusAdded.setText("↗ 已添加");  // 对标截图：“↗ 已添加”
            }
        } else {
            holder.tvReqGreeting.setText("对方: " + greeting);
            // 分支 B：是别人申请加我的
            if (item.getStatus() == 0) {
                // 对方申请，我未处理 -> 展示绿色“接受”小药丸按钮
                holder.btnAccept.setVisibility(View.VISIBLE);
                holder.tvStatusAdded.setVisibility(View.GONE);
                // 绑定“接受”点击回调
                holder.btnAccept.setOnClickListener(v -> {
                    if (acceptClickListener != null) {
                        acceptClickListener.onAcceptClick(item.getId(), position);
                    }
                });
            } else if (item.getStatus() == 1){
                // 对方申请，我已同意 -> 展示“已添加”置灰字样
                holder.btnAccept.setVisibility(View.GONE);
                holder.tvStatusAdded.setVisibility(View.VISIBLE);
                holder.tvStatusAdded.setText("已添加");
            }
        }

        // 异步加载头像 (推荐配合您项目中的 Glide 使用)
        /*
        if (item.getSenderAvatar() != null) {
            Glide.with(context)
                 .load(item.getSenderAvatar())
                 .placeholder(R.drawable.default_avatar)
                 .into(holder.ivReqAvatar);
        }
        */
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        ImageView ivReqAvatar;
        TextView tvReqName;
        TextView tvReqGreeting;
        MaterialButton btnAccept;
        TextView tvStatusAdded;

        public RequestViewHolder(@NonNull View view) {
            super(view);
            ivReqAvatar = view.findViewById(R.id.iv_req_avatar);
            tvReqName = view.findViewById(R.id.tv_req_name);
            tvReqGreeting = view.findViewById(R.id.tv_req_greeting);
            btnAccept = view.findViewById(R.id.btn_accept);
            tvStatusAdded = view.findViewById(R.id.tv_status_added);
        }
    }
}
