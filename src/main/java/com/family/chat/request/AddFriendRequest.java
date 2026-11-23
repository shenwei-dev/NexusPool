package com.family.chat.request;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AddFriendRequest {

    private Long targetUserId;

    private String targetMessage;
}
