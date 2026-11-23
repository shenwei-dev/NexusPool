package com.family.chat.service.impl;

import com.family.chat.common.result.ResultCode;
import com.family.chat.entity.Friend;
import com.family.chat.entity.FriendRequest;
import com.family.chat.enums.FriendRequestStatus;
import com.family.chat.exception.ServiceException;
import com.family.chat.mapper.FriendMapper;
import com.family.chat.mapper.FriendRequestMapper;
import com.family.chat.request.AddFriendRequest;
import com.family.chat.request.UpdateFriendRemarkRequest;
import com.family.chat.service.FriendsService;
import com.family.chat.service.OssService;
import com.family.chat.util.SecurityUtil;
import com.family.chat.vo.QueryFriendDetailVO;
import com.family.chat.vo.QueryFriendListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendsService {

    private final FriendMapper friendMapper;

    private final FriendRequestMapper friendRequestMapper;

    private final OssService ossService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFriend(AddFriendRequest addFriendRequest) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long targetUserId = addFriendRequest.getTargetUserId();

        if (friendMapper.exists(userId, targetUserId) > 0) {
            throw new ServiceException(ResultCode.FRIEND_ALREADY_EXISTS);
        }

        if (friendRequestMapper.existsPendingRequest(userId, targetUserId)) {
            throw new ServiceException(ResultCode.FRIEND_REQUEST_PENDING);
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setFromUserId(userId);
        friendRequest.setToUserId(targetUserId);
        friendRequest.setStatus(FriendRequestStatus.PENDING.getCode());
        friendRequest.setCreatedAt(System.currentTimeMillis());
        friendRequest.setUpdatedAt(System.currentTimeMillis());

        friendRequestMapper.insert(friendRequest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRemark(UpdateFriendRemarkRequest updateFriendRemarkRequest) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long friendId = updateFriendRemarkRequest.getFriendId();
        if (friendMapper.exists(userId, friendId) == 0) {
            throw new ServiceException(ResultCode.FRIEND_NOT_FOUND);
        }
        friendMapper.updateRemark(userId, friendId, updateFriendRemarkRequest.getRemark());
    }

    @Override
    public void deleteFriend(Long friendId) {
        Long userId = SecurityUtil.getCurrentUserId();

        if (friendMapper.exists(userId, friendId) == 0) {
            throw new ServiceException(ResultCode.FRIEND_NOT_FOUND);
        }

        friendMapper.deleteFriend(userId, friendId, System.currentTimeMillis());
    }

    @Override
    public List<QueryFriendListVO> listFriends(Long userId) {
        List<QueryFriendListVO> friendList = friendMapper.selectFriendList(userId);
        // Sign avatar object keys into accessible URLs before leaving the service layer
        friendList.forEach(vo -> vo.setAvatarUrl(ossService.resolveAvatarUrl(vo.getAvatarUrl())));
        return friendList;
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        return friendMapper.exists(userId, friendId) > 0;
    }

    @Override
    public QueryFriendDetailVO getFriend(Long friendId) {
        QueryFriendDetailVO detail = friendMapper.selectFriendDetail(SecurityUtil.getCurrentUserId(), friendId);
        if (detail != null) {
            detail.setAvatarUrl(ossService.resolveAvatarUrl(detail.getAvatarUrl()));
        }
        return detail;
    }
}
