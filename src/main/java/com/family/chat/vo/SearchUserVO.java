package com.family.chat.vo;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SearchUserVO {

    private Long account;

    private String nickname;

    private String avatarUrl;
}
