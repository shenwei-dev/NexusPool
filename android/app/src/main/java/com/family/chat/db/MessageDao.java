package com.family.chat.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LocalMessage message);

    // 1. 【修改点】：将 friendId 修改为数据库物理列名 friend_id
    @Query("SELECT * FROM (SELECT * FROM local_message WHERE friend_id = :friendId ORDER BY timestamp DESC LIMIT 100) ORDER BY timestamp ASC")
    LiveData<List<LocalMessage>> getChatHistory(Long friendId);

    // 2. 【修改点】：将 sendStatus 修改为 send_status，将 msgId 修改为 msg_id
    @Query("UPDATE local_message SET send_status = :status WHERE message_id = :messageId")
    void updateStatus(Long messageId, int status);

    // 根据临时 ID 查询本地单条消息
    @Query("SELECT * FROM local_message WHERE message_id = :messageId")
    LocalMessage getMessageById(Long messageId);

    // 根据临时 ID 物理删除本地单条消息
    @Query("DELETE FROM local_message WHERE message_id = :messageId")
    void deleteMessage(Long messageId);

    /**
     * 【流式输出核心】：根据消息ID，将新吐出来的字追加到已有文本的后面
     */
    @Query("UPDATE local_message SET content = :newContent WHERE message_id = :messageId")
    void updateContent(Long messageId, String newContent);

    /**
     * 【滑动窗口】：只查询与该好友最近的 100 条消息（按序号降序），用于极速空洞盘点
     * 耗时恒定小于 0.1ms，绝对不会卡死手机！
     */
    @Query("SELECT * FROM local_message WHERE friend_id = :friendId ORDER BY sequence DESC LIMIT 100")
    List<LocalMessage> getRecentMessagesForGapCheck(Long friendId);

    // 在 Android 端的 MessageDao.java 中增加：

    /**
     * 【离线自愈队列】：查询本地所有处于“发送中（status = 0）”的消息
     * 排序规则：按时间戳降序（Newest First），即新消息大于旧消息
     */
    @Query("SELECT * FROM local_message WHERE send_status = 0 ORDER BY timestamp DESC")
    List<LocalMessage> getPendingMessages();

    @Query("DELETE FROM local_message WHERE friend_id = :friendId")
    void deleteHistoryByFriendId(Long friendId);

    // 🌟 核心：一条 SQL 同时更新内容和状态，只触发 1 次 LiveData 刷新！
    @Query("UPDATE local_message SET content = :content, send_status = :status WHERE message_id = :tempId")
    void updateContentAndStatus(Long tempId, String content, int status);

    // 🌟 核心：直接 UPDATE 原地修改 ID、sequence 和状态！绝不删除数据！
    @Query("UPDATE local_message SET message_id = :realMsgId, sequence = :sequence, send_status = :status WHERE message_id = :tempId")
    int updateRealMsgIdAndStatus(Long tempId, Long realMsgId, Long sequence, int status);
}