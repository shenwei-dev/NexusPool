package com.family.chat.entity;

import com.family.chat.enums.FriendRequestStatus;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class FriendRequest {

    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private Integer status;

    private Integer deleted;

    private Long deletedAt;

    private Long createdAt;

    private Long updatedAt;

    private String greeting;

    private String remark;
}
