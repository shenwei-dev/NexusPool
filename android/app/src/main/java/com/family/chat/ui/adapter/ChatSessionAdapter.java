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
import com.family.chat.vo.ChatSessionVO;
import com.family.chat.util.FontUtil;

import java.util.ArrayList;
import java.util.List;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.SessionViewHolder> {

    private List<ChatSessionVO> dataList = new ArrayList<>();
    private OnItemClickListener itemClickListener;
    private OnItemLongClickListener itemLongClickListener;

    public interface OnItemClickListener {
        void onItemClick(ChatSessionVO session);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setNewData(List<ChatSessionVO> list) {
        this.dataList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(ChatSessionVO session, View view);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.itemLongClickListener = listener;
    }


    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ChatSessionVO item = dataList.get(position);
        if (item == null) return;
        holder.tvSessionTitle.setText(item.getDisplayName());
        String lastMessage = item.getLastMessage();
        if (lastMessage != null
                && lastMessage.contains("distance")
                && lastMessage.contains("latitude")
                && lastMessage.contains("longitude")
                && lastMessage.contains("roadAddress")
                && lastMessage.contains("title")) {
            lastMessage = "[位置消息]";
        } else if (lastMessage != null && lastMessage.contains("chat/images/")) {
            // 图片消息内容为对象 key(chat/images/...)或含该路径的签名 URL, 统一归一化预览文案
            lastMessage = "[图片]";
        }
        FontUtil.setWeChatStyleEllipsize(holder.tvSessionLastMsg, lastMessage);
        holder.tvSessionTime.setText(item.getFormatTime());
        if (item.getUnreadCount() > 0) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadBadge.setText(String.valueOf(item.getUnreadCount()));
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }
        if (item.getFriendId() != null) {
            // 会话列表携带最新头像: 加载并同步写入全局缓存
            AvatarManager.put(item.getFriendId(), item.getAvatarUrl());
            AvatarManager.load(holder.ivSessionAvatar, item.getFriendId());
        } else {
            holder.ivSessionAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(item);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (itemLongClickListener != null) {
                itemLongClickListener.onItemLongClick(item, v);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSessionAvatar;
        TextView tvUnreadBadge;
        TextView tvSessionTitle;
        TextView tvSessionLastMsg;
        TextView tvSessionTime;

        public SessionViewHolder(@NonNull View view) {
            super(view);
            ivSessionAvatar = view.findViewById(R.id.iv_session_avatar);
            tvUnreadBadge = view.findViewById(R.id.tv_unread_badge);
            tvSessionTitle = view.findViewById(R.id.tv_session_title);
            tvSessionLastMsg = view.findViewById(R.id.tv_session_last_msg);
            tvSessionTime = view.findViewById(R.id.tv_session_time);
        }
    }
}