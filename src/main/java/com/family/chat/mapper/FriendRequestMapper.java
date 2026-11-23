package com.family.chat.mapper;

import com.family.chat.entity.FriendRequest;
import com.family.chat.vo.FriendRequestVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendRequestMapper {

    @Insert("""
        INSERT INTO im_friend_request
        (id, from_user_id, to_user_id, created_at, updated_at, greeting, remark)
        VALUES
        (#{id}, #{fromUserId}, #{toUserId}, #{createdAt}, #{updatedAt}, #{greeting}, #{remark})
    """)
    int insert(FriendRequest request);

    @Select("""
        SELECT * FROM im_friend_request
        WHERE id = #{id}
    """)
    FriendRequest selectById(@Param("id") Long id);

    @Select("""
        SELECT * FROM im_friend_request
        WHERE (
            (from_user_id = #{fromUserId} AND to_user_id = #{toUserId})
            OR
            (from_user_id = #{toUserId} AND to_user_id = #{fromUserId})
        )
        AND status = 0
        LIMIT 1
    """)
    FriendRequest selectExist(@Param("fromUserId") Long fromUserId,
                              @Param("toUserId") Long toUserId);

    @Update("""
        UPDATE im_friend_request
        SET status = #{status},
            updated_at = #{updatedAt}
        WHERE id = #{id}
    """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("updatedAt") Long updatedAt);

    @Update("""
        UPDATE im_friend_request
        SET deleted = 1,
            deleted_at = #{deletedAt},
            updated_at = #{deletedAt}
        WHERE id = #{id}
          AND deleted = 0
    """)
    int deleteLogic(@Param("id") Long id,
                    @Param("deletedAt") Long deletedAt);

    @Select("""
        SELECT * FROM im_friend_request
        WHERE to_user_id = #{toUserId}
        ORDER BY created_at DESC
    """)
    List<FriendRequest> selectIncoming(@Param("toUserId") Long toUserId);

    @Update("""
        UPDATE im_friend_request
        SET deleted = 1,
            deleted_at = #{deletedAt},
            updated_at = #{deletedAt}
        WHERE to_user_id = #{userId}
          AND deleted = 0
    """)
    int clearIncomingRequests(@Param("userId") Long userId,
                              @Param("deletedAt") Long deletedAt);

    @Select("""
        SELECT * FROM im_friend_request
        WHERE from_user_id = #{fromUserId}
        ORDER BY created_at DESC
    """)
    List<FriendRequest> selectOutgoing(@Param("fromUserId") Long fromUserId);

    /**
     * 判断两人之间是否存在待处理申请。
     * 性能要点: 双向查询拆成 UNION ALL, 两个分支分别命中 idx_from_to 与反向索引, 存在即短路返回
     */
    @Select("""
        SELECT EXISTS(
            SELECT 1 FROM (
                (SELECT id FROM im_friend_request
                 WHERE from_user_id = #{userId} AND to_user_id = #{targetUserId} AND status = 0 LIMIT 1)
                UNION ALL
                (SELECT id FROM im_friend_request
                 WHERE from_user_id = #{targetUserId} AND to_user_id = #{userId} AND status = 0 LIMIT 1)
            ) AS pending_match
        )
        """)
    boolean existsPendingRequest(
            @Param("userId") Long userId,
            @Param("targetUserId") Long targetUserId
    );

    @Select("SELECT COUNT(*) FROM im_friend_request WHERE to_user_id = #{userId} AND status = 0")
    int selectUnreadCount(@Param("userId") Long userId);

// 在后端的 FriendRequestMapper.java 中修改：

    /**
     * 【大厂级动态连表查询】：双向查询我发出和收到的所有申请，并利用 CASE WHEN 动态、高能、无延迟地拼接【对方】的最新的昵称和头像
     */
    @Select("""
        SELECT 
            r.id AS id,
            r.from_user_id AS fromUserId,
            r.greeting AS greeting,
            r.remark AS remark,
            r.status AS status,
            u.nickname AS senderNickname, -- 👈 动态拼接出的对方的最新昵称
            u.avatar_url AS senderAvatarUrl     -- 👈 动态拼接出的对方的最新头像
        FROM im_friend_request r
        LEFT JOIN im_user u ON u.id = (
            CASE 
                WHEN r.from_user_id = #{userId} THEN r.to_user_id 
                ELSE r.from_user_id 
            END
        )
        WHERE (r.from_user_id = #{userId} OR r.to_user_id = #{userId})
        AND r.deleted = 0
        ORDER BY r.created_at DESC
    """)
    List<FriendRequestVO> selectUndelete(@Param("userId") Long userId);
}
