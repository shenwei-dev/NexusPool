package com.family.chat.repository;

import com.family.chat.api.FriendRequestApi;
import com.family.chat.api.RetrofitClient;
import com.family.chat.common.ApiResponse;
import com.family.chat.vo.QueryFriendRequestListVO;
import com.family.chat.dto.SendFriendRequestDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRequestRepository {

    private FriendRequestApi friendRequestApi;

    public FriendRequestRepository() {
        friendRequestApi = RetrofitClient.getInstance().create(FriendRequestApi.class);
    }

    public void sendFriendRequest(SendFriendRequestDTO sendFriendRequestDTO, FriendRequestRepository.FriendRequestCallback<ApiResponse<Void>> callback){
        friendRequestApi.sendFriendRequest(
                sendFriendRequestDTO)
                .enqueue(
                        new Callback<ApiResponse<Void>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response){
                                handleResponse(response, callback);
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<Void>> call, Throwable t){
                                callback.onFailure("网络异常");
                            }
                        });
    }

    public void getFriendRequestList(FriendRequestRepository.FriendRequestCallback<ApiResponse<List<QueryFriendRequestListVO>>> callback){
        friendRequestApi.getFriendRequestList()
                .enqueue(
                        new Callback<ApiResponse<List<QueryFriendRequestListVO>>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<List<QueryFriendRequestListVO>>> call, Response<ApiResponse<List<QueryFriendRequestListVO>>> response){
                                handleResponse(response, callback);
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<List<QueryFriendRequestListVO>>> call, Throwable t){
                                callback.onFailure("网络异常");
                            }
                        });
    }

    public void acceptFriendRequest(Long id, FriendRequestRepository.FriendRequestCallback<ApiResponse<Void>> callback){
        friendRequestApi.acceptFriendRequest(id)
                .enqueue(
                        new Callback<ApiResponse<Void>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response){
                                handleResponse(response, callback);
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<Void>> call, Throwable t){
                                callback.onFailure("网络异常");
                            }
                        });
    }
    /** 统一处理响应 */
    private <T> void handleResponse(Response<ApiResponse<T>> response, FriendRequestRepository.FriendRequestCallback<ApiResponse<T>> callback){
        // 如果HTTP请求状态码为200-299
        if(response.isSuccessful() && response.body()!=null){
            ApiResponse<T> result = response.body();
            // 如果业务状态码为1000(表示请求正常)
            if(result.getCode()==1000){
                callback.onSuccess(result);
            }else{
                callback.onFailure(result.getMessage());
            }
        }else{
            callback.onFailure("服务器异常");
        }
    }

    public interface FriendRequestCallback<T>{

        void onSuccess(T response);

        void onFailure(String error);
    }
}
