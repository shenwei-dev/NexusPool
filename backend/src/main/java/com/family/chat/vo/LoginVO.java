package com.family.chat.vo;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LoginVO {

    private Long userId;

    private Long account;

    private String nickname;

    private String avatarUrl;

    /* 访问Token **/
    private String accessToken;

    /* 刷新Token **/
    private String refreshToken;
}
