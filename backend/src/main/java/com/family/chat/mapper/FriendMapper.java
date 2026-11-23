package com.family.chat.mapper;

import com.family.chat.entity.Friend;
import com.family.chat.vo.QueryFriendDetailVO;
import com.family.chat.vo.QueryFriendListVO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FriendMapper {

    @Insert("""
        INSERT INTO im_friend
        (id, user_id, friend_id, remark, created_at)
        VALUES
        (#{id}, #{userId}, #{friendId}, #{remark}, #{createdAt})
    """)
    int insert(Friend friend);

    @Select("""
    SELECT f.* 
    FROM im_friend f
    JOIN im_user u ON f.friend_id = u.id
    WHERE f.user_id = #{userId}
      AND f.friend_id = #{friendId}
      AND u.deleted = 0
    LIMIT 1
""")
    Friend selectOne(@Param("userId") Long userId,
                     @Param("friendId") Long friendId);

    @Select("""
    SELECT f.friend_id, f.remark, u.avatar_url, u.nickname
    FROM im_friend f
    JOIN im_user u ON f.friend_id = u.id
    WHERE f.user_id = #{userId}
      AND f.deleted = 0
""")
    List<QueryFriendListVO> selectFriendList(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(1)
            FROM im_friend
            WHERE 
                (user_id = #{userId} AND friend_id = #{friendId})
            OR
                (user_id = #{friendId} AND friend_id = #{userId})
            """)
    int exists(
            @Param("userId") Long userId,
            @Param("friendId") Long friendId
    );

    @Update("""
            UPDATE im_friend
            SET 
                deleted = 1,
                deleted_at = #{deletedAt}
            WHERE
                user_id = #{userId}
            AND
                friend_id = #{friendId}
            AND
                deleted = 0
            """)
    int deleteFriend(
            @Param("userId") Long userId,
            @Param("friendId") Long friendId,
            @Param("deletedAt") Long deletedAt
    );

    @Update("""
            UPDATE im_friend
            SET
                remark = #{remark}
            WHERE
                user_id = #{userId}
            AND
                friend_id = #{friendId}
            AND
                deleted = 0
            """)
    int updateRemark(
            @Param("userId") Long userId,
            @Param("friendId") Long friendId,
            @Param("remark") String remark
    );

    @Select("""
    SELECT 
        f.friend_id,
        f.remark,
        u.nickname,
        u.avatar_url,
        u.account
    FROM im_friend f
    JOIN im_user u 
        ON f.friend_id = u.id
    WHERE f.user_id = #{userId}
      AND f.friend_id = #{friendId}
      AND u.deleted = 0
    LIMIT 1
""")
    QueryFriendDetailVO selectFriendDetail(
            @Param("userId") Long userId,
            @Param("friendId") Long friendId
    );
}