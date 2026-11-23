package com.family.chat.service;

import com.family.chat.dto.QueryFriendRequestListDTO;
import com.family.chat.entity.FriendRequest;
import com.family.chat.request.FriendAcceptRequest;
import com.family.chat.request.FriendApplyRequest;
import com.family.chat.vo.FriendRequestVO;
import io.netty.channel.Channel;

import java.util.List;

public interface FriendRequestService {

    // 发送请求
    void transmitFriendRequest(FriendApplyRequest friendApplyRequest);

    // 同意请求
    void acceptFriendRequest(Long id);

    // 查询请求列表
    List<FriendRequestVO> getFriendRequestList();

    void pushOfflineFriendRequests(Long userId, Channel channel);
}
