package com.family.chat.entity;

import com.family.chat.enums.UserStatus;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private Long id;

    private Long account;

    private String passwordHash;

    private String nickname;

    private String avatarUrl;

//    private UserStatus status;

    private Integer status;

    private Long createdAt;

    private Long updatedAt;

    private Boolean deleted;

    private Long deletedAt;

    private Integer privacyMode;
}