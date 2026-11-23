package com.family.chat.api;

import com.family.chat.common.ApiResponse;
import com.family.chat.vo.ChatSessionVO;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ChatSessionApi {
    // 拉取会话列表
    @GET("/api/chat/session/list")
    Call<ApiResponse<List<ChatSessionVO>>> getRecentSessions();

    // 进入聊天窗口时调用，清除未读
    @POST("/api/chat/session/clear-unread")
    Call<ApiResponse<Void>> clearUnreadCount(@Query("friendId") Long friendId);

    @POST("/api/chat/session/pin")
    Call<ApiResponse<Void>> togglePin(@Query("friendId") Long friendId, @Query("isPinned") int isPinned);

    @POST("/api/chat/session/hide")
    Call<ApiResponse<Void>> hideSession(@Query("friendId") Long friendId);

    @POST("/api/chat/session/delete")
    Call<ApiResponse<Void>> deleteSessionAndHistory(@Query("friendId") Long friendId);
}