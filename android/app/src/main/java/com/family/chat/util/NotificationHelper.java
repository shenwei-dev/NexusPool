package com.family.chat.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.family.chat.R;
import com.family.chat.ui.activity.ChatActivity;
import com.family.chat.ui.activity.AddFriendActivity;

public class NotificationHelper {

    /** 消息渠道ID */
    private static final String CHANNEL_CHAT_ID = "chat_messages_v3";

    /** 好友申请渠道ID */
    private static final String CHANNEL_FRIEND_ID = "friend_requests_v3";  // 好友申请渠道 ID

    /** 消息通知ID */
    private static final int NOTIFICATION_CHAT_ID = 1001;

    /** 好友申请通知ID */
    private static final int NOTIFICATION_FRIEND_ID = 1002;

    /**
     * 初始化创建通知渠道
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                // 获取系统默认的提示音
                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                // 配置音频播放属性：设置为即时通讯消息提醒
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .build();

                // 👈 【核心重构二】：微信同款双震动频率（不延迟 0ms ➡️ 震 150ms ➡️ 停 80ms ➡️ 再震 150ms）
                long[] wechatVibratePattern = new long[]{0, 150, 80, 150};

                // A. 聊天消息渠道 (高优先级：支持屏幕顶部横幅弹出)
                NotificationChannel chatChannel = new NotificationChannel(
                        CHANNEL_CHAT_ID,
                        "聊天消息",
                        NotificationManager.IMPORTANCE_HIGH
                );
                chatChannel.setDescription("用于接收并弹出好友发来的即时聊天消息");
                chatChannel.enableVibration(true); // 开启震动
                chatChannel.setVibrationPattern(wechatVibratePattern); // 绑定微信双震动
                chatChannel.setSound(defaultSoundUri, audioAttributes); // 绑定声音

                // B. 好友申请渠道 (高优先级：支持屏幕顶部横幅弹出)
                NotificationChannel friendChannel = new NotificationChannel(
                        CHANNEL_FRIEND_ID,
                        "好友申请",
                        NotificationManager.IMPORTANCE_HIGH
                );
                friendChannel.setDescription("用于接收新好友添加申请通知");

                manager.createNotificationChannel(chatChannel);
                manager.createNotificationChannel(friendChannel);
            }
        }
    }

    /**
     * 弹出聊天消息横幅通知
     */
    public static void showChatNotification(Context context, Long senderId, String senderName, String content) {
        // 确保通知渠道已创建
        createNotificationChannels(context);

        // 微信级规范：点击通知直接跳转到与该好友的 ChatActivity，并清空中间栈
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("friend_id", senderId);
        intent.putExtra("nickname", senderName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // 处理 Android 12+ 的 PendingIntent 安全强类型 Flag 兼容性
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        // 使用发送者 ID 作为 requestCode，防止不同好友的通知点击发生跳转混淆
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, (int) (long) senderId, intent, pendingFlags);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_CHAT_ID)
                .setSmallIcon(R.drawable.ic_tab_chat) // 使用我们精致的微信小图标
                .setContentTitle(senderName)          // 标题显示发送人姓名
                .setContentText(content)              // 内容显示聊天文本
                .setPriority(NotificationCompat.PRIORITY_HIGH) // 强行设为高优先级，支持顶部悬浮横幅
                .setDefaults(NotificationCompat.DEFAULT_ALL)   // 开启默认的声音和震动
                .setAutoCancel(true)                  // 点击后通知自动消失
                .setContentIntent(pendingIntent)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 150, 80, 150});
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_CHAT_ID, builder.build());
        } catch (SecurityException e) {
            Log.w("NotificationHelper", "发送通知失败, 缺少通知权限", e);
        }
    }

    /**
     * 弹出新好友申请横幅通知
     */
    public static void showFriendRequestNotification(Context context, String senderName, String greeting) {
        createNotificationChannels(context);

        // 点击通知直接跳转到“新的朋友（AddFriendActivity）”页面
        Intent intent = new Intent(context, AddFriendActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_FRIEND_ID)
                .setSmallIcon(R.drawable.ic_tab_contacts)
                .setContentTitle("新的朋友")
                .setContentText('"' + senderName + '"' + " 申请添加您为好友")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 150, 80, 150});
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_FRIEND_ID, builder.build());
        } catch (SecurityException e) {
            Log.w("NotificationHelper", "发送通知失败, 缺少通知权限", e);
        }
    }
}