package com.family.chat.repository;

import android.util.Log;

import com.family.chat.api.ChatSessionApi;
import com.family.chat.api.RetrofitClient;
import com.family.chat.common.ApiResponse;
import com.family.chat.vo.ChatSessionVO;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatSessionRepository {

    private final ChatSessionApi chatSessionApi;

    public ChatSessionRepository() {
        chatSessionApi = RetrofitClient.getInstance().create(ChatSessionApi.class);
    }

    /** 统一处理响应 */
    private <T> void handleResponse(Response<ApiResponse<T>> response, ChatSessionRepository.ChatSessionCallback<ApiResponse<T>> callback){
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
    public void getRecentSessions(ChatSessionRepository.ChatSessionCallback<ApiResponse<List<ChatSessionVO>>> callback){
        chatSessionApi.getRecentSessions()
                .enqueue(
                        new Callback<ApiResponse<List<ChatSessionVO>>>() {
                            @Override
                            public void onResponse(
                                    Call<ApiResponse<List<ChatSessionVO>>> call,
                                    Response<ApiResponse<List<ChatSessionVO>>> response
                            ){

                                handleResponse(
                                        response,
                                        callback
                                );

                            }

                            @Override
                            public void onFailure(
                                    Call<ApiResponse<List<ChatSessionVO>>> call,
                                    Throwable t
                            ){

                                callback.onFailure(
                                        "网络异常"
                                );

                            }

                        });


    }

    public void clearUnreadCount(Long friendId) {
        chatSessionApi.clearUnreadCount(friendId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("ChatSessionRepository", "清除未读数失败", t);
            }
        });
    }
    public interface ChatSessionCallback<T>{

        void onSuccess(T response);

        void onFailure(String error);
    }

    public void togglePin(Long contactId, int isPinned, ChatSessionCallback<ApiResponse<Void>> callback) {
        chatSessionApi.togglePin(contactId, isPinned).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body());
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void hideSession(Long contactId, ChatSessionCallback<ApiResponse<Void>> callback) {
        chatSessionApi.hideSession(contactId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body());
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void deleteSessionAndHistory(Long contactId, ChatSessionCallback<ApiResponse<Void>> callback) {
        chatSessionApi.deleteSessionAndHistory(contactId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body());
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }
}
