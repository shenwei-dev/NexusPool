package com.family.chat.api;

import com.family.chat.common.ApiResponse;
import com.family.chat.dto.UserLoginDTO;
import com.family.chat.vo.UserLoginVO;
import com.family.chat.dto.UserRegisterDTO;
import com.family.chat.vo.UserRegisterVO;
import com.family.chat.dto.UserRefreshTokenDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

// 有关认证的Api
public interface AuthApi {

    // 注册
    @POST("/api/auth/register")
    Call<ApiResponse<UserRegisterVO>> register(@Body UserRegisterDTO userRegisterDTO);

    // 登录
    @POST("/api/auth/login")
    Call<ApiResponse<UserLoginVO>> login(@Body UserLoginDTO userLoginDTO);

    // 使用刷新令牌静默换取新的访问令牌
    @POST("/api/auth/refresh")
    Call<ApiResponse<UserLoginVO>> refresh(@Body UserRefreshTokenDTO userRefreshTokenDTO);
}
