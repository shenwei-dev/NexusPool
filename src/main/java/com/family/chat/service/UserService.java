package com.family.chat.service;

import com.family.chat.dto.SearchUserDTO;
import com.family.chat.dto.QueryUserInfoDTO;

public interface UserService {

    QueryUserInfoDTO getUserById();

    SearchUserDTO queryUserByAccount(Long account);

    /**
     * Persist the avatar object key and return a signed accessible URL.
     */
    String updateAvatar(String avatarObjectKey);

    void updateNickname(String nickname);
}
