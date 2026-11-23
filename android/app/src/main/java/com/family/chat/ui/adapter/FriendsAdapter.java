package com.family.chat.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.family.chat.R;
import com.family.chat.util.AvatarManager;
import com.family.chat.vo.FriendVO;
import java.util.ArrayList;
import java.util.List;

// 1. 让类显式继承 RecyclerView.Adapter
public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    /** 存储好友列表数据的集合 */
    private List<FriendVO> dataList = new ArrayList<>();

    /** 在好友项上的点击事件监听器 */
    private OnItemClickListener listener;

    /** 更新好友列表数据 */
    public void setNewData(List<FriendVO> list) {
        if (list != null) {
            this.dataList = list;
        } else {
            this.dataList.clear();
        }
        notifyDataSetChanged();
    }

    /** 初始化点击事件监听器 */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /** 获取列表中的项目 */
    public FriendVO getItem(int position) {
        if (position >= 0 && position < dataList.size()) {
            return dataList.get(position);
        }
        return null;
    }

    /** 获取列表中项目数量 */
    @Override
    public int getItemCount() {
        return dataList.size();
    }

    /** 获取好友数量 */
    public int getFriendCount() {
        return dataList.size();
    }

    /** 实现标准的 onCreateViewHolder，用于加载好友单项布局 */
    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_list, parent, false);
        return new FriendViewHolder(view);
    }

    /** 实现标准的 onBindViewHolder，用于绑定单条好友数据 */
    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        // 获取好友项
        FriendVO friend = getItem(position);
        if (friend != null) {
            String displayName = friend.getDisplayName();
            // 将好友昵称渲染到TextView中
            holder.tvName.setText(displayName);

            // 本条数据携带的最新签名 URL 先写入全局缓存,
            // 再统一走 AvatarManager.load: 本地固定路径文件命中即秒开, 未命中才走网络
            AvatarManager.put(friend.getFriendId(), friend.getAvatarUrl());
            AvatarManager.load(holder.ivAvatar, friend.getFriendId());

            // 绑定点击事件
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(friend, position);
                }
            });
        }
    }

    // 修改接口定义：增加 FriendVO 参数，方便调用方直接获取数据
    public interface OnItemClickListener {
        void onItemClick(FriendVO friend, int position);
    }

    // 7. 定义好友单项的 ViewHolder
    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivAvatar;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_contact_name);       // 好友昵称TextView的ID
            ivAvatar = itemView.findViewById(R.id.iv_contact_avatar);   // item_contact_list 布局中的头像控件ID
        }
    }
}