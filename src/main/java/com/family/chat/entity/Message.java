package com.family.chat.entity;

import com.family.chat.enums.MessageStatus;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    /** 消息唯一ID */
    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private Long sequence;

    private String content;

    private Integer messageType;

    private Integer status;

    private Long createdAt;

    private Long updatedAt;
}
