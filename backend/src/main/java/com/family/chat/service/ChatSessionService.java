package com.family.chat.service;

import com.family.chat.vo.ChatSessionVO;

import java.util.List;

public interface ChatSessionService {
    List<ChatSessionVO> getRecentSessions(Long userId);

    void clearUnreadCount(Long userId, Long friendId);

    void togglePin(Long userId, Long friendId, int pinned);

    void hideSession(Long userId, Long friendId);

    void deleteSessionAndHistory(Long userId, Long friendId);
}