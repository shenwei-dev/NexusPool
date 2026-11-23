package com.family.chat.vo;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class QueryUserInfoVO {

    private Long account;

    private String nickname;

    private String avatarUrl;
}
