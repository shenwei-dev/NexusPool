package com.family.chat.entity;

import com.family.chat.enums.FriendStatus;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Friend {

    private Long id;

    private Long userId;

    private Long friendId;

    private String remark;

    private Integer deleted;

    private Long deletedAt;

    private Long createdAt;

    private Long updatedAt;
}
