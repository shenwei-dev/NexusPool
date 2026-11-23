package com.family.chat.vo;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionVO {

    private Long sessionId;      // 会话唯一ID
    private Long friendId;      // 对方的UserId
    private String displayName;  // 👈 经过数据库判定后的展示名称（备注 > 昵称）
    private String avatarUrl;       // 对方的最新头像
    private String lastMessage;  // 最后一条消息内容
    private Integer unreadCount; // 未读消息数
    private Long updatedAt;      // 更新时间戳 (用于客户端排序和显示时间)
    private Integer pinned;
}