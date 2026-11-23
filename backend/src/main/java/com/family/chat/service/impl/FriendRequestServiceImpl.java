package com.family.chat.service.impl;

import com.family.chat.common.result.ResultCode;
import com.family.chat.entity.*;
import com.family.chat.enums.FriendRequestStatus;
import com.family.chat.exception.ServiceException;
import com.family.chat.mapper.*;
import com.family.chat.netty.SessionManager;
import com.family.chat.protobuf.MessageProto;
import com.family.chat.request.FriendApplyRequest;
import com.family.chat.service.FriendRequestService;
import com.family.chat.service.OssService;
import com.family.chat.util.SecurityUtil;
import com.family.chat.util.UserIdentifierGenerator;
import com.family.chat.vo.FriendRequestVO;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 好友申请服务实现。
 *
 * <p>负责好友申请的发起、接受，以及在线用户的实时推送提示；同时负责在用户上线时补推离线期间的未读申请数。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FriendRequestServiceImpl implements FriendRequestService {

    /** 好友申请数据访问 */
    private final FriendRequestMapper friendRequestMapper;

    /** 好友关系数据访问 */
    private final FriendMapper friendMapper;

    /** 用户数据访问 */
    private final UserMapper userMapper;

    /** 在线会话管理器：用于定位目标用户的 Channel */
    private final SessionManager sessionManager;

    /** 会话数据访问：用于创建初始会话 */
    private final ChatSessionMapper chatSessionMapper;

    /** 对象存储服务：用于签名头像等静态资源 */
    private final OssService ossService;

    /**
     * 发起好友申请，并在目标用户在线时实时推送提示。
     *
     * @param friendApplyRequest 好友申请参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transmitFriendRequest(FriendApplyRequest friendApplyRequest) {
        Long targetAccount = friendApplyRequest.getTargetAccount();

        User user = userMapper.selectByAccount(targetAccount);
        if (user == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }

        Long fromUserId = SecurityUtil.getCurrentUserId();
        Long toUserId = user.getId();

        FriendRequest existRequest = friendRequestMapper.selectExist(fromUserId, toUserId);
        if (existRequest != null) {
            throw new ServiceException(ResultCode.FRIEND_REQUEST_PENDING);
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setId(UserIdentifierGenerator.nextId());
        friendRequest.setFromUserId(fromUserId);
        friendRequest.setToUserId(toUserId);
        friendRequest.setCreatedAt(System.currentTimeMillis());
        friendRequest.setUpdatedAt(System.currentTimeMillis());
        friendRequest.setRemark(friendApplyRequest.getRemark());
        friendRequest.setGreeting(friendApplyRequest.getMessage());

        friendRequestMapper.insert(friendRequest);
        log.info("Friend request persisted. [fromUserId={}, toUserId={}]", fromUserId, toUserId);

        Channel receiverChannel = sessionManager.getChannel(toUserId);
        if (receiverChannel != null && receiverChannel.isActive()) {
            log.info("Receiver online, pushing real-time badge signal. [toUserId={}]", toUserId);

            MessageProto.MsgFrame pushProto = MessageProto.MsgFrame.newBuilder()
                    .setSenderId(fromUserId)
                    .setContent(friendApplyRequest.getMessage())
                    .setType(6)
                    .setSequence(1L)
                    .build();

            receiverChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(pushProto.toByteArray())));
            log.info("Badge signal pushed to receiver. [toUserId={}]", toUserId);
        } else {
            log.info("Receiver offline, skip pushing, will be pulled after login. [toUserId={}]", toUserId);
        }
    }

    /**
     * 接受好友申请并建立双向好友关系，同时初始化双方会话并推送“新会话”提示。
     *
     * @param id 好友申请记录 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptFriendRequest(Long id) {
        FriendRequest friendRequest = friendRequestMapper.selectById(id);
        if (friendRequest == null) {
            throw new ServiceException(ResultCode.FRIEND_REQUEST_NOT_EXIST);
        }
        friendRequestMapper.updateStatus(id, FriendRequestStatus.ACCEPTED.getCode(), System.currentTimeMillis());

        Long userA = friendRequest.getFromUserId();
        Long userB = friendRequest.getToUserId();
        insertFriendRelation(userA, userB, friendRequest.getRemark());
        insertFriendRelation(userB, userA, null);

        String initMessage = "您已添加了对方为好友，现在可以开始聊天了。";
        createChatSession(userA, userB, initMessage);
        createChatSession(userB, userA, initMessage);

        pushNewSessionSignal(userA, userB, initMessage);
        pushNewSessionSignal(userB, userA, initMessage);
    }

    /**
     * 查询当前用户的好友申请列表。
     *
     * <p>在离开服务层前将头像对象 Key 转为可访问 URL。
     *
     * @return 好友申请列表
     */
    @Override
    public List<FriendRequestVO> getFriendRequestList() {
        List<FriendRequestVO> requests = friendRequestMapper.selectUndelete(SecurityUtil.getCurrentUserId());
        // Sign avatar object keys into accessible URLs before leaving the service layer
        requests.forEach(vo -> vo.setSenderAvatarUrl(ossService.resolveAvatarUrl(vo.getSenderAvatarUrl())));
        return requests;
    }

    /**
     * 插入单向好友关系。
     *
     * @param fromUserId 发起方用户 ID
     * @param toUserId   对端用户 ID
     * @param remark     备注（可为空）
     */
    private void insertFriendRelation(Long fromUserId, Long toUserId, String remark) {
        Friend friend = new Friend();
        friend.setId(UserIdentifierGenerator.nextId());
        friend.setUserId(fromUserId);
        friend.setFriendId(toUserId);
        friend.setRemark(remark);
        friend.setCreatedAt(System.currentTimeMillis());
        friendMapper.insert(friend);
    }

    /**
     * 初始化用户会话记录。
     *
     * @param userId      会话所属用户 ID
     * @param friendId    对端用户 ID
     * @param lastMessage 初始化会话展示的最后一条消息
     */
    private void createChatSession(Long userId, Long friendId, String lastMessage) {
        ChatSession session = new ChatSession();
        session.setId(UserIdentifierGenerator.nextId());
        session.setUserId(userId);
        session.setFriendId(friendId);
        session.setLastMessage(lastMessage);
        session.setUnreadCount(0);
        session.setUpdatedAt(System.currentTimeMillis());
        chatSessionMapper.insert(session);
    }

    /**
     * 向在线用户推送“新增会话/好友关系”提示。
     *
     * @param fromUserId 需要推送的用户 ID
     * @param toUserId   新增好友 ID
     * @param lastMessage 会话展示的最新消息
     */
    private void pushNewSessionSignal(Long fromUserId, Long toUserId, String lastMessage) {
        Channel channel = sessionManager.getChannel(fromUserId);
        if (channel != null && channel.isActive()) {
            MessageProto.MsgFrame pushProto = MessageProto.MsgFrame.newBuilder()
                    .setType(7)
                    .setReceiverId(toUserId)
                    .setTimestamp(System.currentTimeMillis())
                    .setLastMessage(lastMessage)
                    .build();
            channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(pushProto.toByteArray())));
        }
    }

    /**
     * 在用户上线握手完成后补推离线期间的未读好友申请数。
     *
     * @param userId  用户 ID
     * @param channel 用户当前在线 Channel
     */
    @Override
    public void pushOfflineFriendRequests(Long userId, Channel channel) {
        try {
            int unreadCount = friendRequestMapper.selectUnreadCount(userId);
            if (unreadCount > 0) {
                log.info("User just came online, re-pushing pending friend request badges. [userId={}, pendingCount={}]",
                        userId, unreadCount);

                MessageProto.MsgFrame pushProto = MessageProto.MsgFrame.newBuilder()
                        .setReceiverId(userId)
                        .setContent("您有未处理的好友申请")
                        .setType(6)
                        .setUnreadCount(unreadCount)
                        .build();

                channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(pushProto.toByteArray())));
            }
        } catch (Exception e) {
            log.error("Failed to re-push offline friend request badges.", e);
        }
    }
}
