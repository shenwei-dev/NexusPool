package com.family.chat.api;

import com.family.chat.common.ApiResponse;
import com.family.chat.vo.OssUploadCredentialVO;
import com.family.chat.vo.QueryUserInfoVO;
import com.family.chat.vo.SearchUserVO;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * 用户相关接口, HTTP 方法与后端 UserController/AuthController/OssController 保持一致
 */
public interface UserApi {

    /** 获取当前登录用户信息 */
    @GET("/api/user/info")
    Call<ApiResponse<QueryUserInfoVO>> getUserInfo();

    /** 按账号精确查询用户 */
    @GET("/api/user/search/{account}")
    Call<ApiResponse<SearchUserVO>> searchUser(@Path("account") Long account);

    /** 退出登录 */
    @POST("/api/auth/logout")
    Call<ApiResponse<Void>> logout();

    /** 获取头像直传凭证: objectKey + 短时效预签名 PUT 地址 */
    @GET("/api/oss/avatar/presign")
    Call<ApiResponse<OssUploadCredentialVO>> getAvatarUploadCredential();

    /** 修改用户头像, body: {"avatarUrl": "<objectKey>"}, 返回签名后的可访问 URL */
    @PUT("/api/user/updateAvatar")
    Call<ApiResponse<String>> updateAvatar(@Body Map<String, String> body);

    /** 修改用户昵称, body: {"nickname": "..."} */
    @PUT("/api/user/updateNickname")
    Call<ApiResponse<Void>> updateNickname(@Body Map<String, String> body);
}
