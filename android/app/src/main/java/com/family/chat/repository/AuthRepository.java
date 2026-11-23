package com.family.chat.repository;

import android.util.Log;

import com.family.chat.ChatApplication;
import com.family.chat.api.AuthApi;
import com.family.chat.api.RetrofitClient;
import com.family.chat.common.ApiResponse;
import com.family.chat.dto.UserLoginDTO;
import com.family.chat.util.SessionManager;
import com.family.chat.vo.UserLoginVO;
import com.family.chat.dto.UserRegisterDTO;
import com.family.chat.vo.UserRegisterVO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    /** 有关认证的Api对象 */
    private final AuthApi authApi;

    /** 构造方法 */
    public AuthRepository() {
        authApi = RetrofitClient.getInstance().create(AuthApi.class);
    }

    /** 注册 */
    public void register(
            UserRegisterDTO userRegisterDTO,
            AuthCallback<ApiResponse<UserRegisterVO>> callback
    ) {

        authApi.register(userRegisterDTO)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(
                            Call<ApiResponse<UserRegisterVO>> call, Response<ApiResponse<UserRegisterVO>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<UserRegisterVO> result = response.body();
                            if (result.getCode() == 1000) {
                                callback.onSuccess(result);
                            } else {
                                callback.onFailure(result.getMessage());
                            }
                        } else {
                            callback.onFailure("注册失败，服务器异常");
                        }
                    }


                    @Override
                    public void onFailure(Call<ApiResponse<UserRegisterVO>> call, Throwable t) {
                        Log.e("AuthRepository", "网络请求失败", t);
                        callback.onFailure("网络请求失败：" + t.getMessage());
                    }
                });
    }

    /** 登录 */
    public void login(UserLoginDTO userLoginDTO, AuthCallback<ApiResponse<UserLoginVO>> callback) {
        authApi.login(userLoginDTO).enqueue(new Callback<ApiResponse<UserLoginVO>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserLoginVO>> call, Response<ApiResponse<UserLoginVO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<UserLoginVO> result = response.body();
                    if (result.getCode() == 1000) {
                        // 获取访问令牌
                        String accessToken = result.getData().getAccessToken();

                        // 获取刷新令牌
                        String refreshToken = result.getData().getRefreshToken();

                        // 获取userId
                        Long userId = result.getData().getUserId();

                        // 获取account
                        Long account = result.getData().getAccount();

                        String nickname = result.getData().getNickname();
                        
                        SessionManager.saveSession(accessToken, refreshToken, userId, account, nickname);
                        callback.onSuccess(result);
                    } else {
                        callback.onFailure(result.getMessage());
                    }
                } else {
                    callback.onFailure("登录失败，服务器异常");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserLoginVO>> call, Throwable t) {
                Log.e("AuthRepository", "网络请求失败", t);
                callback.onFailure("网络请求失败：" + t.getMessage());
            }
        });
    }

    public interface AuthCallback<T>{

        void onSuccess(T response);

        void onFailure(String error);
    }
}
