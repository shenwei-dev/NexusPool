package com.family.chat.request;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class FriendAcceptRequest {

    private Long targetAccount;
}
