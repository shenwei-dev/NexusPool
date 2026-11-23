package com.family.chat.mapper;

import com.family.chat.entity.ChatSession;
import com.family.chat.vo.ChatSessionVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatSessionMapper {

    @Insert("""
        INSERT INTO im_chat_session
        (id, user_id, friend_id, last_message, unread_count, updated_at)
        VALUES
        (#{id}, #{userId}, #{friendId}, #{lastMessage}, #{unreadCount}, #{updatedAt})
    """)
    int insert(ChatSession chatSession);

    /**
     * 【重构：大厂排序算法】：置顶（pinned）优先降序，其次按活跃时间（updated_at）降序
     */
    @Select("""
        SELECT 
            s.id AS sessionId,
            s.friend_id AS friendId,
            s.last_message AS lastMessage,
            s.unread_count AS unreadCount,
            s.updated_at AS updatedAt,
            s.pinned AS pinned, 
            u.avatar_url AS avatarUrl,
            COALESCE(f.remark, u.nickname) AS displayName
        FROM im_chat_session s
        INNER JOIN im_user u ON s.friend_id = u.id
        LEFT JOIN im_friend f ON s.user_id = f.user_id AND s.friend_id = f.friend_id
        WHERE s.user_id = #{userId}
        ORDER BY s.pinned DESC, s.updated_at DESC -- 置顶永远排在最上面
    """)
    List<ChatSessionVO> selectUserSessions(@Param("userId") Long userId);

    /**
     * 【核心改动一】：当收到新消息时，在数据库中将接收方的未读数累加 1
     */
    @Update("""
        UPDATE im_chat_session 
        SET last_message = #{content}, 
            unread_count = unread_count + 1, 
            updated_at = #{updatedAt} 
        WHERE user_id = #{userId} AND friend_id = #{friendId}
    """)
    int incrementUnreadCount(@Param("userId") Long userId,
                             @Param("friendId") Long friendId,
                             @Param("content") String content,
                             @Param("updatedAt") Long updatedAt);

    /**
     * 【核心改动二】：当用户进入聊天窗口阅读了消息，在数据库中将该会话未读数清零
     */
    @Update("""
        UPDATE im_chat_session 
        SET unread_count = 0 
        WHERE user_id = #{userId} AND friend_id = #{friendId}
    """)
    int clearUnreadCount(@Param("userId") Long userId,
                         @Param("friendId") Long friendId);

    /**
     * 更新发送方自己的会话，未读数保持 0 不变
     */
    @Update("""
        UPDATE im_chat_session 
        SET last_message = #{content}, 
            updated_at = #{timestamp} 
        WHERE user_id = #{userId} AND friend_id = #{friendId}
    """)
    int updateSenderSession(@Param("userId") Long userId,
                            @Param("friendId") Long friendId,
                            @Param("content") String content,
                            @Param("timestamp") Long timestamp);

    /**
     * 更新置顶状态
     */
    @Update("""
        UPDATE im_chat_session 
        SET pinned = #{pinned}, 
            updated_at = #{timestamp} 
        WHERE user_id = #{userId} AND friend_id = #{friendId}
    """)
    int updateSessionPinned(@Param("userId") Long userId,
                            @Param("friendId") Long friendId,
                            @Param("pinned") int pinned,
                            @Param("timestamp") Long timestamp);

    /**
     * 【不显示该聊天】：物理删除这一行会话（不影响物理消息明细表 im_message）
     */
    @Delete("DELETE FROM im_chat_session WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int deleteSession(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
