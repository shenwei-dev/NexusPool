package com.family.chat.vo;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class QueryFriendListVO {

    private Long friendId;

    private String nickname;

    private String remark;

    private String avatarUrl;
}
