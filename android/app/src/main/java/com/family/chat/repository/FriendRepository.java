package com.family.chat.repository;

import com.family.chat.api.FriendApi;
import com.family.chat.api.RetrofitClient;
import com.family.chat.common.ApiResponse;
import com.family.chat.vo.FriendDetailVO;
import com.family.chat.vo.FriendVO;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 好友数据仓库。
 *
 * <p>只保留与后端 FriendController 实际对应的能力: 列表 / 详情 / 删除。
 * 添加好友与搜索走 FriendRequest 与 User 模块, 此处不再冗余封装。
 */
public class FriendRepository {

    private final FriendApi friendApi;

    public FriendRepository() {
        friendApi = RetrofitClient.getInstance().create(FriendApi.class);
    }

    /**
     * 删除好友
     */
    public void deleteFriend(Long friendId, FriendCallback<ApiResponse<Void>> callback) {
        friendApi.deleteFriend(friendId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        handleResponse(response, callback);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        callback.onFailure("网络异常");
                    }
                });
    }

    /**
     * 查询好友列表
     */
    public void getFriendList(FriendCallback<ApiResponse<List<FriendVO>>> callback) {
        friendApi.getFriendList()
                .enqueue(new Callback<ApiResponse<List<FriendVO>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<FriendVO>>> call, Response<ApiResponse<List<FriendVO>>> response) {
                        handleResponse(response, callback);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<FriendVO>>> call, Throwable t) {
                        callback.onFailure("网络异常");
                    }
                });
    }

    /**
     * 查询好友详情
     */
    public void getFriendDetail(Long friendId, FriendCallback<ApiResponse<FriendDetailVO>> callback) {
        friendApi.getFriendDetail(friendId)
                .enqueue(new Callback<ApiResponse<FriendDetailVO>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<FriendDetailVO>> call, Response<ApiResponse<FriendDetailVO>> response) {
                        handleResponse(response, callback);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<FriendDetailVO>> call, Throwable t) {
                        callback.onFailure("网络异常");
                    }
                });
    }

    /** 统一处理响应 */
    private <T> void handleResponse(Response<ApiResponse<T>> response, FriendCallback<ApiResponse<T>> callback) {
        // 如果HTTP请求状态码为200-299
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse<T> result = response.body();
            // 如果业务状态码为1000(表示请求正常)
            if (result.getCode() == 1000) {
                callback.onSuccess(result);
            } else {
                callback.onFailure(result.getMessage());
            }
        } else {
            callback.onFailure("服务器异常");
        }
    }

    public interface FriendCallback<T> {

        void onSuccess(T response);

        void onFailure(String error);
    }
}
