package com.family.chat.vo;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class FriendRequestVO {

    private Long id;

    private Long fromUserId;

    private Integer status;

    private String greeting;

    private String remark;

    private String senderNickname;

    private String senderAvatarUrl;
}
