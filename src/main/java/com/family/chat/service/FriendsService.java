package com.family.chat.service;


import com.family.chat.entity.Friend;
import com.family.chat.request.AddFriendRequest;
import com.family.chat.request.UpdateFriendRemarkRequest;
import com.family.chat.vo.QueryFriendDetailVO;
import com.family.chat.vo.QueryFriendListVO;

import java.util.List;

/**
 * 联系人服务接口 (ContactsService)
 *
 * @author ShenWei
 */
public interface FriendsService {


    void addFriend(AddFriendRequest addFriendRequest);
    void updateRemark(UpdateFriendRemarkRequest updateFriendRemarkRequest);
    void deleteFriend(Long contactId);
    List<QueryFriendListVO> listFriends(Long userId);
    boolean isFriend(Long userId, Long contactUserId);

    QueryFriendDetailVO getFriend(Long friendId);
}


