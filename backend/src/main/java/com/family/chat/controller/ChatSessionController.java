package com.family.chat.controller;

import com.family.chat.common.result.ApiResponse;
import com.family.chat.service.ChatSessionService;
import com.family.chat.util.SecurityUtil;
import com.family.chat.vo.ChatSessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/session")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 拉取当前登录用户的所有活跃聊天会话
     */
    @GetMapping("/list")
    public ApiResponse<List<ChatSessionVO>> getRecentSessions() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        List<ChatSessionVO> sessions = chatSessionService.getRecentSessions(currentUserId);
        return ApiResponse.success(sessions);
    }

    /**
     * 进入聊天窗口时调用：将对应的会话未读数清零
     */
    @PostMapping("/clear-unread")
    public ApiResponse<Void> clearUnread(@RequestParam("friendId") Long friendId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        chatSessionService.clearUnreadCount(currentUserId, friendId);
        return ApiResponse.success();
    }

    /**
     * 置顶或取消置顶该聊天
     */
    @PostMapping("/pin")
    public ApiResponse<Void> togglePin(@RequestParam("friendId") Long friendId, @RequestParam("isPinned") int pinned) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        chatSessionService.togglePin(currentUserId, friendId, pinned);
        return ApiResponse.success();
    }

    /**
     * 不显示该聊天
     */
    @PostMapping("/hide")
    public ApiResponse<Void> hideSession(@RequestParam("friendId") Long friendId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        chatSessionService.hideSession(currentUserId, friendId);
        return ApiResponse.success();
    }

    /**
     * 删除该聊天 (删除首屏会话 + 物理清空云端消息明细)
     */
    @PostMapping("/delete")
    public ApiResponse<Void> deleteSessionAndHistory(@RequestParam("friendId") Long friendId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        chatSessionService.deleteSessionAndHistory(currentUserId, friendId);
        return ApiResponse.success();
    }
}
