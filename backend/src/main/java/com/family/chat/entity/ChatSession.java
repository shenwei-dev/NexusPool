package com.family.chat.entity;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ChatSession {

    private Long id;

    private Long userId;

    private Long friendId;

    private String lastMessage;

    private Integer pinned;

    private Integer unreadCount;

    private Long updatedAt;
}
