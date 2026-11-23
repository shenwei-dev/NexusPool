package com.family.chat.request;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageRequest {

    private Long toUserId;

    private String message;
}
