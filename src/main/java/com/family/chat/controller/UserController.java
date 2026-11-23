package com.family.chat.controller;

import com.family.chat.common.result.ApiResponse;
import com.family.chat.dto.SearchUserDTO;
import com.family.chat.request.UpdateAvatarUrlRequest;
import com.family.chat.request.UpdateNicknameRequest;
import com.family.chat.service.UserService;
import com.family.chat.vo.QueryUserInfoVO;
import com.family.chat.vo.SearchUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    public ApiResponse<QueryUserInfoVO> queryUserInfo() {
        QueryUserInfoVO queryUserInfoVO = new QueryUserInfoVO();
        BeanUtils.copyProperties(userService.getUserById(), queryUserInfoVO);
        return ApiResponse.success(queryUserInfoVO);
    }

    @GetMapping("/search/{account}")
    public ApiResponse<SearchUserVO> searchUser(@PathVariable("account") Long account) {
        SearchUserDTO searchUserDTO = userService.queryUserByAccount(account);
        if (searchUserDTO != null) {
            SearchUserVO searchUserVO = new SearchUserVO();
            BeanUtils.copyProperties(searchUserDTO, searchUserVO);
            return ApiResponse.success(searchUserVO);
        }
        return ApiResponse.success(null);
    }

    @PutMapping("/updateAvatar")
    public ApiResponse<String> updateAvatar(@Valid @RequestBody UpdateAvatarUrlRequest request) {
        String signedAvatarUrl = userService.updateAvatar(request.getAvatarUrl());
        return ApiResponse.success(signedAvatarUrl);
    }

    @PutMapping("/updateNickname")
    public ApiResponse<Void> updateNickname(@Valid @RequestBody UpdateNicknameRequest request) {
        userService.updateNickname(request.getNickname());
        return ApiResponse.success();
    }
}
