package com.family.chat.service.impl;

import com.family.chat.mapper.ChatSessionMapper;
import com.family.chat.mapper.MessageMapper;
import com.family.chat.service.ChatSessionService;
import com.family.chat.service.OssService;
import com.family.chat.vo.ChatSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;

    private final MessageMapper messageMapper;

    private final OssService ossService;

    @Override
    public List<ChatSessionVO> getRecentSessions(Long userId) {
        List<ChatSessionVO> sessions = chatSessionMapper.selectUserSessions(userId);
        // Sign avatar object keys into accessible URLs before leaving the service layer
        sessions.forEach(vo -> vo.setAvatarUrl(ossService.resolveAvatarUrl(vo.getAvatarUrl())));
        return sessions;
    }

    @Override
    public void clearUnreadCount(Long userId, Long friendId) {
        chatSessionMapper.clearUnreadCount(userId, friendId);
    }

    @Override
    public void togglePin(Long userId, Long friendId, int pinned) {
        chatSessionMapper.updateSessionPinned(userId, friendId, pinned, System.currentTimeMillis());
    }

    @Override
    public void hideSession(Long userId, Long friendId) {
        chatSessionMapper.deleteSession(userId, friendId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSessionAndHistory(Long userId, Long friendId) {
        chatSessionMapper.deleteSession(userId, friendId);
        messageMapper.deleteChatHistory(userId, friendId);
    }
}
