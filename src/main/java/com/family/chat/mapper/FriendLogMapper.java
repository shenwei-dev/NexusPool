package com.family.chat.mapper;

import com.family.chat.entity.FriendLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendLogMapper {

    @Insert("""
        INSERT INTO im_friend_log
        (id, user_id, target_user_id, action, request_id, created_at)
        VALUES
        (#{id}, #{userId}, #{targetUserId}, #{action}, #{requestId}, #{createdAt})
    """)
    int insert(FriendLog log);

    @Select("""
        SELECT * FROM im_friend_log
        WHERE user_id = #{userId}
        ORDER BY created_at DESC
    """)
    List<FriendLog> selectByUserId(@Param("userId") Long userId);
}
