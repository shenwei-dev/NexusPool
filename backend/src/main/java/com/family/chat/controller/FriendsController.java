package com.family.chat.controller;

import com.family.chat.common.result.ApiResponse;
import com.family.chat.entity.Friend;
import com.family.chat.request.AddFriendRequest;
import com.family.chat.request.UpdateFriendRemarkRequest;
import com.family.chat.service.FriendsService;
import com.family.chat.util.SecurityUtil;
import com.family.chat.vo.QueryFriendDetailVO;
import com.family.chat.vo.QueryFriendListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friends")
public class FriendsController {

    /** 联系人服务层 */
    private final FriendsService friendsService;

    @PostMapping("/add")
    public ApiResponse<Void> addFriend(@Valid @RequestBody AddFriendRequest addFriendRequest) {
        friendsService.addFriend(addFriendRequest);
        return ApiResponse.success();
    }

    @DeleteMapping("/delete/{friendId}")
    public ApiResponse<Void> deleteFriend(@PathVariable("friendId") Long friendId) {
        friendsService.deleteFriend(friendId);
        return ApiResponse.success();
    }

    @PutMapping("/remark")
    public ApiResponse<Void> updateRemark(@Valid @RequestBody UpdateFriendRemarkRequest updateFriendRemarkRequest) {
        friendsService.updateRemark(updateFriendRemarkRequest);
        return ApiResponse.success();
    }

    @GetMapping("/list")
    public ApiResponse<List<QueryFriendListVO>> listFriends() {
        return ApiResponse.success(friendsService.listFriends(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/detail/{friendId}")
    public ApiResponse<QueryFriendDetailVO> queryFriendDetail(@PathVariable("friendId") Long friendId) {
        return ApiResponse.success(friendsService.getFriend(friendId));
    }
}
