package com.family.chat.entity;

import com.family.chat.enums.FriendAction;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class FriendLog {

    private Long id;

    private Long userId;

    private Long targetUserId;

    private FriendAction action;

    private Long requestId;

    private Long createdAt;
}
