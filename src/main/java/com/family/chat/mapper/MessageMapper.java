package com.family.chat.mapper;

import com.family.chat.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("""
    INSERT INTO im_message
    (id, from_user_id, to_user_id, content, sequence, message_type, status, created_at, updated_at)
    VALUES
    (#{id}, #{fromUserId}, #{toUserId}, #{content}, #{sequence}, #{messageType}, #{status}, #{createdAt}, #{updatedAt})
""")
    int insert(Message message);

    @Update("""
    UPDATE im_message
    SET status = #{status},
        updated_at = #{updatedAt}
    WHERE id = #{id}
""")
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("updatedAt") Long updatedAt);

    @Select("""
    SELECT * FROM im_message
    WHERE to_user_id = #{toUserId}
    AND status = 0
    ORDER BY created_at ASC
""")
    List<Message> selectUnread(@Param("toUserId") Long toUserId);

    @Select("""
        SELECT COUNT(*) 
        FROM im_message
        WHERE to_user_id = #{toUserId}
          AND status = 0
    """)
    int countUnread(@Param("toUserId") Long toUserId);

    /**
     * 【大厂级区间查询】：根据高水位线，向后查询最新的增量消息，用于客户端自愈。
     * 性能要点: 双向会话拆成 UNION ALL 两个分支, 各自走 idx_session_seq(from_user_id,to_user_id,sequence)
     * 做索引范围扫描, 避免 OR 条件导致的 index_merge 不确定性
     */
    @Select("""
        SELECT * FROM (
            (SELECT * FROM im_message
             WHERE from_user_id = #{userId} AND to_user_id = #{friendId} AND sequence > #{localMaxSeq}
             ORDER BY sequence ASC LIMIT 100)
            UNION ALL
            (SELECT * FROM im_message
             WHERE from_user_id = #{friendId} AND to_user_id = #{userId} AND sequence > #{localMaxSeq}
             ORDER BY sequence ASC LIMIT 100)
        ) AS merged
        ORDER BY sequence ASC LIMIT 100
    """)
    List<Message> selectDeltaMessages(@Param("userId") Long userId,
                                      @Param("friendId") Long friendId,
                                      @Param("localMaxSeq") Long localMaxSeq);

    /**
     * 【RAG 记忆体查询】：查询双方最近的 N 条聊天记录，并在外层将其按时间升序（正序）排列，完美对准大模型上下文规范。
     * 性能要点: 按 sequence 排序可直接命中 idx_session_seq(from_user_id,to_user_id,sequence) 的索引有序性,
     * 两个分支各自走索引倒序扫描 LIMIT 后归并, 全程无 filesort 无全表扫描
     */
    @Select("""
        SELECT * FROM (
            SELECT * FROM (
                (SELECT * FROM im_message
                 WHERE from_user_id = #{userId} AND to_user_id = #{friendId}
                 ORDER BY sequence DESC LIMIT #{limit})
                UNION ALL
                (SELECT * FROM im_message
                 WHERE from_user_id = #{friendId} AND to_user_id = #{userId}
                 ORDER BY sequence DESC LIMIT #{limit})
            ) AS merged
            ORDER BY sequence DESC LIMIT #{limit}
        ) AS temp_table
        ORDER BY sequence ASC
    """)
    List<Message> selectRecentHistory(@Param("userId") Long userId,
                                      @Param("friendId") Long friendId,
                                      @Param("limit") int limit);

    // 在 MessageMapper.java 中增加：
    @Delete("""
        DELETE FROM im_message 
        WHERE (from_user_id = #{userId} AND to_user_id = #{friendId}) 
        OR (from_user_id = #{friendId} AND to_user_id = #{userId})
    """)
    void deleteChatHistory(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
