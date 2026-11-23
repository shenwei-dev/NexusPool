package com.family.chat.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class QueryFriendRequestListDTO {

    private Long id;

    private Long fromUserId;

    private Integer status;

    private String remark;

    private String greeting;

    private String senderNickname;

    private String senderAvatarUrl;
}
