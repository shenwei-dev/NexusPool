package com.family.chat.api;

import com.family.chat.common.ApiResponse;
import com.family.chat.vo.FriendDetailVO;
import com.family.chat.vo.FriendVO;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * 好友关系接口, 与后端 FriendsController 保持一致
 */
public interface FriendApi {

    /** 查询好友列表 */
    @GET("/api/friends/list")
    Call<ApiResponse<List<FriendVO>>> getFriendList();

    /** 查询好友详细信息 */
    @GET("/api/friends/detail/{friendId}")
    Call<ApiResponse<FriendDetailVO>> getFriendDetail(@Path("friendId") Long friendId);

    /** 删除好友 */
    @DELETE("/api/friends/delete/{friendId}")
    Call<ApiResponse<Void>> deleteFriend(@Path("friendId") Long friendId);
}
