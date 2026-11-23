package com.family.chat.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.family.chat.R;
import com.family.chat.constant.MessageType;
import com.family.chat.model.ChatMessage;
import com.family.chat.model.LocationMessage;
import com.family.chat.ui.activity.ImagePreviewActivity;
import com.family.chat.util.AmapStaticMapUtil;
import com.family.chat.util.AvatarManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TEXT_SEND = 1;
    private static final int TYPE_TEXT_RECEIVE = 2;
    private static final int TYPE_LOCATION_SEND = 3;
    private static final int TYPE_LOCATION_RECEIVE = 4;
    private static final int TYPE_IMAGE_SEND = 5;
    private static final int TYPE_IMAGE_RECEIVE = 6;
    private List<ChatMessage> dataList = new ArrayList<>();
    private OnBubbleLongClickListener bubbleLongClickListener;

    public interface OnBubbleLongClickListener {
        void onBubbleLongClick(ChatMessage message, View view);
    }

    public void setOnBubbleLongClickListener(OnBubbleLongClickListener listener) {
        this.bubbleLongClickListener = listener;
    }

    /**
     * 🌟 优化后的更新数据方法 (利用 DiffUtil 实现局部精准刷新)
     */
    public void setNewData(List<ChatMessage> newList) {
        if (newList == null) {
            newList = new ArrayList<>();
        }
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChatMessageDiffCallback(this.dataList, newList));
        this.dataList.clear();
        this.dataList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = dataList.get(position);
        boolean isMe = message.isMe();

        // 判断消息类型
        if (message.getMessageType() == MessageType.IMAGE) {
            return isMe ? TYPE_IMAGE_SEND : TYPE_IMAGE_RECEIVE; // 👈 图片消息
        } else if (message.getMessageType() == MessageType.LOCATION) {
            return isMe ? TYPE_LOCATION_SEND : TYPE_LOCATION_RECEIVE; // 定位消息
        } else {
            return isMe ? TYPE_TEXT_SEND : TYPE_TEXT_RECEIVE; // 普通文本
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_IMAGE_SEND) {
            View view = inflater.inflate(R.layout.item_chat_msg_image_sent, parent, false);
            return new ImageSendViewHolder(view);
        } else if (viewType == TYPE_IMAGE_RECEIVE) {
            View view = inflater.inflate(R.layout.item_chat_msg_image_received, parent, false);
            return new ImageReceiveViewHolder(view);
        } else if (viewType == TYPE_LOCATION_SEND) {
            // 👈 1. 加载我发送定位的卡片布局
            View view = inflater.inflate(R.layout.item_chat_msg_location_sent, parent, false);
            return new LocationSendViewHolder(view);

        } else if (viewType == TYPE_LOCATION_RECEIVE) {
            // 👈 2. 加载对方发送定位的卡片布局
            View view = inflater.inflate(R.layout.item_chat_msg_location_received, parent, false);
            return new LocationReceiveViewHolder(view);

        } else if (viewType == TYPE_TEXT_SEND) {
            View view = inflater.inflate(R.layout.item_chat_send, parent, false);
            return new SendViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_receive, parent, false);
            return new ReceiveViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = dataList.get(position);
        if (holder instanceof ImageSendViewHolder) {
            bindSendImageMessage((ImageSendViewHolder) holder, message);
        } else if (holder instanceof ImageReceiveViewHolder) {
            bindReceiveImageMessage((ImageReceiveViewHolder) holder, message);
        } else if (holder instanceof SendViewHolder) {
            SendViewHolder sendHolder = (SendViewHolder) holder;
            sendHolder.tvContent.setText(message.getContent());
            AvatarManager.loadMyAvatar(sendHolder.ivAvatar); // 加载本人真实头像
            sendHolder.tvContent.setOnLongClickListener(v -> {
                if (bubbleLongClickListener != null) {
                    bubbleLongClickListener.onBubbleLongClick(message, v);
                    return true;
                }
                return false;
            });

            int status = message.getSendStatus();
            if (status == ChatMessage.STATUS_SENDING) {
                sendHolder.progressBar.setVisibility(View.VISIBLE);  // 显示转圈
                sendHolder.ivError.setVisibility(View.GONE);         // 隐藏红点
            } else if (status == ChatMessage.STATUS_FAILED) {
                sendHolder.progressBar.setVisibility(View.GONE);     // 隐藏转圈
                sendHolder.ivError.setVisibility(View.VISIBLE);      // 显示红点
            } else {
                sendHolder.progressBar.setVisibility(View.GONE);     // 隐藏转圈
                sendHolder.ivError.setVisibility(View.GONE);         // 隐藏红点
            }

        } else if (holder instanceof ReceiveViewHolder) {
            ReceiveViewHolder receiveHolder = (ReceiveViewHolder) holder;
            receiveHolder.tvContent.setText(message.getContent());
            receiveHolder.tvContent.setOnLongClickListener(v -> {
                if (bubbleLongClickListener != null) {
                    bubbleLongClickListener.onBubbleLongClick(message, v);
                    return true;
                }
                return false;
            });
            // 加载对方真实头像 (AI 助手特判内置于 AvatarManager)
            AvatarManager.load(receiveHolder.ivAvatar, message.getReceiverId());
        } else if (holder instanceof LocationSendViewHolder) {
            bindSendLocationMessage((LocationSendViewHolder) holder, message);
        } else if (holder instanceof LocationReceiveViewHolder) {
            bindReceiveLocationMessage((LocationReceiveViewHolder) holder, message);
        }
    }

    /**
     * 右侧：我发送的消息 ViewHolder
     */
    static class SendViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        ImageView ivAvatar;
        ProgressBar progressBar;
        ImageView ivError;

        public SendViewHolder(@NonNull View view) {
            super(view);
            tvContent = view.findViewById(R.id.tv_send_content);
            ivAvatar = view.findViewById(R.id.iv_send_avatar);
            progressBar = view.findViewById(R.id.progress_sending);
            ivError = view.findViewById(R.id.iv_send_error);
        }
    }

    /**
     * 左侧：对方发送的消息 ViewHolder
     */
    static class ReceiveViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        ImageView ivAvatar;

        public ReceiveViewHolder(@NonNull View view) {
            super(view);
            tvContent = view.findViewById(R.id.tv_receive_content);
            ivAvatar = view.findViewById(R.id.iv_receive_avatar);
        }
    }

    /**
     * 右侧：我发送的【定位消息】ViewHolder
     */
    static class LocationSendViewHolder extends RecyclerView.ViewHolder {
        CardView cardBubble;       // 气泡卡片整体
        TextView tvTitle;          // 主地名 (如：君悦府)
        TextView tvAddress;        // 详细地址/路牌信息
        ImageView ivMapSnapshot;   // 地图静态缩略图
        ImageView ivAvatar;        // 我的头像

        ImageView ivSendFailed;
        ProgressBar progressUpload;
        View viewUploadMask;

        public LocationSendViewHolder(@NonNull View view) {
            super(view);
            cardBubble = view.findViewById(R.id.card_location_bubble);
            tvTitle = view.findViewById(R.id.tv_location_title);
            tvAddress = view.findViewById(R.id.tv_location_address);
            ivMapSnapshot = view.findViewById(R.id.iv_map_snapshot);
            ivAvatar = view.findViewById(R.id.iv_send_avatar);
            ivSendFailed = view.findViewById(R.id.iv_send_failed);
            progressUpload = view.findViewById(R.id.progress_upload);
            viewUploadMask = view.findViewById(R.id.view_upload_mask);
        }
    }

    /**
     * 左侧：对方发送的【定位消息】ViewHolder
     */
    static class LocationReceiveViewHolder extends RecyclerView.ViewHolder {
        CardView cardBubble;       // 气泡卡片整体
        TextView tvTitle;          // 主地名
        TextView tvAddress;        // 详细地址/路牌信息
        ImageView ivMapSnapshot;   // 地图静态缩略图
        ImageView ivAvatar;        // 对方头像

        public LocationReceiveViewHolder(@NonNull View view) {
            super(view);
            cardBubble = view.findViewById(R.id.card_location_bubble);
            tvTitle = view.findViewById(R.id.tv_location_title);
            tvAddress = view.findViewById(R.id.tv_location_address);
            ivMapSnapshot = view.findViewById(R.id.iv_map_snapshot);
            ivAvatar = view.findViewById(R.id.iv_receive_avatar);
        }
    }

    /**
     * 绑定我发出的定位消息 (直接传入外层 ChatMessage)
     */
    private void bindSendLocationMessage(LocationSendViewHolder holder, ChatMessage message) {
        AvatarManager.loadMyAvatar(holder.ivAvatar); // 加载本人真实头像
        // 🌟 1. 核心：直接从外层 ChatMessage 拿到发送状态 (转圈/成功/失败)！
        int status = message.getSendStatus();

        // 🌟 2. 从 message.getContent() 的 JSON 文本解包出内层 LocationMessage 对象
        LocationMessage locationMsg = message.toLocationMessage(); // 或者 new Gson().fromJson(message.getContent(), LocationMessage.class)
        if (locationMsg == null) {
            locationMsg = new LocationMessage("位置信息", "", "", 0, 0); // 防空保护
        }

        // 3. 渲染地名与详细地址
        holder.tvTitle.setText(locationMsg.getTitle());
        holder.tvAddress.setText(locationMsg.getDistance() + locationMsg.getRoadAddress());

        // 4. 加载高德静态地图缩略图
        String staticMapUrl = AmapStaticMapUtil.getStaticMapUrl(locationMsg.getLongitude(), locationMsg.getLatitude());
        Glide.with(holder.itemView.getContext())
                .load(staticMapUrl)
                .placeholder(R.color.grey_light)
                .into(holder.ivMapSnapshot);

        // 🌟 5. 根据外层 ChatMessage 的状态，控制【转圈遮罩】与【红色感叹号】
        if (status == ChatMessage.STATUS_SENDING) {
            // 发送中：显示遮罩 + 转圈
            holder.progressUpload.setVisibility(View.VISIBLE);
            holder.viewUploadMask.setVisibility(View.VISIBLE);
            holder.ivSendFailed.setVisibility(View.GONE);

        } else if (status == ChatMessage.STATUS_SUCCESS) {
            // 发送成功：隐藏遮罩 + 转圈
            holder.progressUpload.setVisibility(View.GONE);
            holder.viewUploadMask.setVisibility(View.GONE);
            holder.ivSendFailed.setVisibility(View.GONE);

        } else if (status == ChatMessage.STATUS_FAILED) {
            // 发送失败：隐藏转圈，显示红色感叹号！
            holder.progressUpload.setVisibility(View.GONE);
            holder.viewUploadMask.setVisibility(View.GONE);
            holder.ivSendFailed.setVisibility(View.VISIBLE);
        }

        // 点击气泡查看大地图
        holder.cardBubble.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "查看位置", Toast.LENGTH_SHORT).show();
        });

        // 失败感叹号点击重发
        holder.ivSendFailed.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "重发定位消息", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 绑定接收方（对方发送的）定位消息数据
     */
    private void bindReceiveLocationMessage(LocationReceiveViewHolder holder, ChatMessage message) {
        if (message == null) return;
        AvatarManager.load(holder.ivAvatar, message.getReceiverId()); // 加载对方真实头像

        LocationMessage locationMsg = message.toLocationMessage();
        if (locationMsg == null) {
            locationMsg = new LocationMessage("位置信息", "", "", 0, 0); // 防空保护
        }

        // 1. 设置主标题与路牌地址信息
        holder.tvTitle.setText(locationMsg.getTitle());
        holder.tvAddress.setText(locationMsg.getDistance() + "|" + locationMsg.getRoadAddress());

        // 2. 使用 Glide 加载高德静态地图缩略图
        String staticMapUrl = AmapStaticMapUtil.getStaticMapUrl(locationMsg.getLongitude(), locationMsg.getLatitude());

        Glide.with(holder.itemView.getContext())
                .load(staticMapUrl)
                .placeholder(android.R.color.darker_gray) // 加载中灰色占位图
                .error(android.R.color.darker_gray)       // 加载失败占位图
                .into(holder.ivMapSnapshot);

        LocationMessage finalLocationMsg = locationMsg;
        holder.cardBubble.setOnClickListener(v -> {
            Context context = v.getContext();
            Toast.makeText(context, "查看对方位置: " + finalLocationMsg.getTitle(), Toast.LENGTH_SHORT).show();

            // 💡 进阶功能：点击直接调起高德地图 APP 给对方导航 (如果手机装了高德地图的话)
            openAmapNavigation(context, finalLocationMsg.getLatitude(), finalLocationMsg.getLongitude(), finalLocationMsg.getTitle());
        });
    }

    private void openAmapNavigation(Context context, double lat, double lng, String title) {
        try {
            Intent intent = new Intent("android.intent.action.VIEW",
                    android.net.Uri.parse("amapuri://route/plan/?dlat=" + lat + "&dlon=" + lng + "&dname=" + title + "&dev=0&t=0"));
            intent.setPackage("com.autonavi.minimap");
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "未安装高德地图 APP，位置: " + title, Toast.LENGTH_SHORT).show();
        }
    }

    static class ImageSendViewHolder extends RecyclerView.ViewHolder {
        CardView cardBubble;
        ImageView ivMsgImage;
        ImageView ivAvatar;
        ImageView ivSendFailed;
        ProgressBar progressUpload;
        View viewUploadMask;

        public ImageSendViewHolder(@NonNull View view) {
            super(view);
            cardBubble = view.findViewById(R.id.card_image_bubble);
            ivMsgImage = view.findViewById(R.id.iv_msg_image);
            ivAvatar = view.findViewById(R.id.iv_send_avatar);
            ivSendFailed = view.findViewById(R.id.iv_send_failed);
            progressUpload = view.findViewById(R.id.progress_upload);
            viewUploadMask = view.findViewById(R.id.view_upload_mask);
        }
    }

    /**
     * 对方发送的【图片消息】ViewHolder
     */
    static class ImageReceiveViewHolder extends RecyclerView.ViewHolder {
        CardView cardBubble;
        ImageView ivMsgImage;
        ImageView ivAvatar;

        public ImageReceiveViewHolder(@NonNull View view) {
            super(view);
            cardBubble = view.findViewById(R.id.card_image_bubble);
            ivMsgImage = view.findViewById(R.id.iv_msg_image);
            ivAvatar = view.findViewById(R.id.iv_receive_avatar);
        }
    }

    private void bindSendImageMessage(ImageSendViewHolder holder, ChatMessage message) {
        AvatarManager.loadMyAvatar(holder.ivAvatar); // 加载本人真实头像
        String content = message.getContent();
        String displayPathOrUrl;
        if (content != null && content.contains("|")) {
            String[] parts = content.split("\\|");
            String localCopyPath = parts[0];
            String remoteUrl = parts[1];
            File localFile = new File(localCopyPath);
            if (localFile.exists() && localFile.length() > 0) {
                displayPathOrUrl = localCopyPath;
            } else {
                displayPathOrUrl = remoteUrl;
            }
        } else {
            displayPathOrUrl = content;
        }

        android.util.Log.e("ChatAdapter", "真正的加载路径: " + displayPathOrUrl);

        // 🌟【核心修复】：判断是网络 URL 还是本地文件！
        // 如果是本地物理路径，必须包装成 new File(...)，否则 Glide 会静默加载失败！
        Object glideTarget;
        if (displayPathOrUrl != null && !displayPathOrUrl.startsWith("http")) {
            glideTarget = new File(displayPathOrUrl); // 👈 告诉 Glide 这是本地物理文件！
        } else {
            glideTarget = displayPathOrUrl; // 👈 告诉 Glide 这是网络图片 URL！
        }

        // 🌟 使用包装后的 glideTarget 加载
        Glide.with(holder.itemView.getContext())
                .load(glideTarget)
                .placeholder(R.color.grey_light)
                .error(android.R.drawable.stat_notify_error) // 加载失败时显示的报错图
                .into(holder.ivMsgImage);

        // 状态处理...
        int status = message.getSendStatus();
        if (status == ChatMessage.STATUS_SENDING) {
            holder.progressUpload.setVisibility(View.VISIBLE);
            holder.viewUploadMask.setVisibility(View.VISIBLE);
            holder.ivSendFailed.setVisibility(View.GONE);
        } else if (status == ChatMessage.STATUS_SUCCESS) {
            holder.progressUpload.setVisibility(View.GONE);
            holder.viewUploadMask.setVisibility(View.GONE);
            holder.ivSendFailed.setVisibility(View.GONE);
        } else if (status == ChatMessage.STATUS_FAILED) {
            holder.progressUpload.setVisibility(View.GONE);
            holder.viewUploadMask.setVisibility(View.GONE);
            holder.ivSendFailed.setVisibility(View.VISIBLE);
        }

        holder.ivSendFailed.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "点击重新发送图片", Toast.LENGTH_SHORT).show();
        });

        holder.cardBubble.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ImagePreviewActivity.class);
            intent.putExtra("image_url", displayPathOrUrl); // 传递当前图片的物理路径或网络 URL
            v.getContext().startActivity(intent);
        });
    }

    private void bindReceiveImageMessage(ImageReceiveViewHolder holder, ChatMessage message) {
        AvatarManager.load(holder.ivAvatar, message.getReceiverId()); // 加载对方真实头像
        String imageUrl = message.getContent();
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(android.R.color.darker_gray)
                .into(holder.ivMsgImage);
        holder.cardBubble.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "点击查看大图", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 🌟 核心：DiffUtil 消息差量对比器
     */
    private static class ChatMessageDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldList;
        private final List<ChatMessage> newList;
        private static final String TAG = "DiffDebug";
        public ChatMessageDiffCallback(List<ChatMessage> oldList, List<ChatMessage> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList != null ? oldList.size() : 0; }

        @Override
        public int getNewListSize() { return newList != null ? newList.size() : 0; }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            ChatMessage oldItem = oldList.get(oldItemPosition);
            ChatMessage newItem = newList.get(newItemPosition);
            boolean isSameItem = Objects.equals(oldItem.getTempId(), newItem.getTempId());
            return isSameItem;
        }
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ChatMessage oldItem = oldList.get(oldItemPosition);
            ChatMessage newItem = newList.get(newItemPosition);
            boolean sameStatus = oldItem.getSendStatus() == newItem.getSendStatus();
            boolean sameContent = Objects.equals(oldItem.getContent(), newItem.getContent());
            boolean sameMsgId = Objects.equals(oldItem.getTempId(), newItem.getTempId());
            boolean isSameContent = sameStatus && sameContent && sameMsgId;
            return isSameContent;
        }
    }

    public void notifyItemChangedByTempId(Long tempId) {
        if (tempId == null) return;
        for (int i = 0; i < dataList.size(); i++) {
            ChatMessage msg = dataList.get(i);
            if (Objects.equals(msg.getTempId(), tempId)) {
                msg.setSendStatus(ChatMessage.STATUS_SUCCESS);
                notifyItemChanged(i);
                Log.d("StatusDebug", "🎯 成功找到 tempId=" + tempId + "，精准刷新第 " + i + " 行！");
                break;
            }
        }
    }
}
