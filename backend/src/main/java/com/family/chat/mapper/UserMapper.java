package com.family.chat.mapper;

import com.family.chat.entity.User;
import com.family.chat.entity.UserSessionProfile;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Insert("""
        INSERT INTO im_user
        (id, account, password_hash, nickname, avatar_url, status, privacy_mode, created_at, updated_at)
        VALUES
        (#{id}, #{account}, #{passwordHash}, #{nickname}, #{avatarUrl}, #{status}, #{privacyMode},  #{createdAt}, #{updatedAt})
    """)
    int insert(User user);

    @Select("SELECT * FROM im_user WHERE id = #{id}")
    User selectById(@Param("id") Long id);

    @Select("SELECT * FROM im_user WHERE account = #{account}")
    User selectByAccount(@Param("account") Long account);

    @Update("""
        UPDATE im_user
        SET nickname = #{nickname},
            avatar_url = #{avatarUrl},
            status = #{status},
            privacy_mode = #{privacyMode},
            updated_at = #{updatedAt}
        WHERE id = #{id}
    """)
    int update(User user);

    @Update("""
        UPDATE im_user
        SET
            avatar_url = #{avatarUrl},
            updated_at = #{updatedAt}
        WHERE id = #{id}
    """)
    int updateAvatarUrl(User user);

    @Update("""
        UPDATE im_user
        SET deleted = 1, deleted_at = #{deletedAt}
        WHERE id = #{id}
    """)
    int deleteLogic(@Param("id") Long id, @Param("deletedAt") Long deletedAt);

    // 在后端的 UserMapper.java 中增加：

    /**
     * 【大厂级名片查询】：在发送消息时，动态查询发送方 A 相对接收方 B 的展示名（备注 > 昵称）和头像
     */
    @Select("""
        SELECT 
            u.id AS userId,
            u.avatar_url AS avatarUrl,
            COALESCE(f.remark, u.nickname) AS displayName
        FROM im_user u
        LEFT JOIN im_friend f ON f.user_id = #{receiverId} AND f.friend_id = #{senderId}
        WHERE u.id = #{senderId}
    """)
    UserSessionProfile selectSessionProfile(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Select("""
        SELECT EXISTS(
            SELECT 1 
            FROM im_user 
            WHERE id = #{id}
        )
        """)
    boolean existsById(@Param("id") Long id);

    @Update("""
        UPDATE im_user
        SET
            nickname = #{nickname},
            updated_at = #{updatedAt}
        WHERE id = #{id}
    """)
    int updateNickname(User user);
}
