package com.family.chat.api;

import com.family.chat.common.ApiResponse;
import com.family.chat.vo.QueryFriendRequestListVO;
import com.family.chat.dto.SendFriendRequestDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface FriendRequestApi {

    // 发送好友请求
    @POST("/api/friend_request/send")
    Call<ApiResponse<Void>> sendFriendRequest(@Body SendFriendRequestDTO sendFriendRequestDTO);

    @GET("/api/friend_request/list")
    Call<ApiResponse<List<QueryFriendRequestListVO>>> getFriendRequestList();

    // 接受好友请求 (后端为 PUT)
    @PUT("/api/friend_request/accept/{id}")
    Call<ApiResponse<Void>> acceptFriendRequest(@Path("id") Long id);
}
