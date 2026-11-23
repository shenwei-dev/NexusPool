package com.family.chat.controller;

import com.family.chat.common.result.ApiResponse;
import com.family.chat.request.FriendApplyRequest;
import com.family.chat.service.FriendRequestService;
import com.family.chat.vo.FriendRequestVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friend_request")
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    @PostMapping("/send")
    public ApiResponse<Void> sendFriendRequest(@Valid @RequestBody FriendApplyRequest friendApplyRequest) {
        friendRequestService.transmitFriendRequest(friendApplyRequest);
        return ApiResponse.success();
    }

    @GetMapping("/list")
    public ApiResponse<List<FriendRequestVO>> getFriendRequestList() {
        return ApiResponse.success(friendRequestService.getFriendRequestList());
    }

    @PutMapping("/accept/{id}")
    public ApiResponse<Void> acceptFriendRequest(@PathVariable("id") Long id) {
        friendRequestService.acceptFriendRequest(id);
        return ApiResponse.success();
    }
}
