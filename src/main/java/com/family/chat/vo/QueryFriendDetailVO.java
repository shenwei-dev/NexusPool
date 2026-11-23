package com.family.chat.vo;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class QueryFriendDetailVO {

    // 好友ID
    private Long friendId;

    // 昵称
    private String nickname;

    // 好友头像
    private String avatarUrl;

    // 备注
    private String remark;

    // 账号
    private Long account;
}
