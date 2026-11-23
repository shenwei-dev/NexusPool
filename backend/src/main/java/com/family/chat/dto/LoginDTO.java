package com.family.chat.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {

    private Long userId;

    private Long account;

    private String nickname;

    private String accessToken;

    private String refreshToken;
}
