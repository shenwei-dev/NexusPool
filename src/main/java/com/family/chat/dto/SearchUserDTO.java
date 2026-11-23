package com.family.chat.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SearchUserDTO {

    private Long account;

    private String nickname;

    private String avatarUrl;
}
