package com.family.chat.repository;

import android.util.Log;

import com.family.chat.api.RetrofitClient;
import com.family.chat.api.UserApi;
import com.family.chat.common.ApiResponse;
import com.family.chat.util.AvatarManager;
import com.family.chat.util.ChatImageUtil;
import com.family.chat.util.SessionManager;
import com.family.chat.vo.OssUploadCredentialVO;
import com.family.chat.vo.QueryUserInfoVO;
import com.family.chat.vo.SearchUserVO;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private static final String TAG = "UserRepository";

    /** 直传对象存储使用裸 OkHttpClient: 不携带业务 Token/拦截器 */
    private static final OkHttpClient DIRECT_UPLOAD_CLIENT = new OkHttpClient();

    private final UserApi userApi;

    public UserRepository() {
        userApi = RetrofitClient.getInstance().create(UserApi.class);
    }

    public void queryUserInfo(UserRepository.UserCallback<ApiResponse<QueryUserInfoVO>> callback){
        userApi.getUserInfo().enqueue(
                        new Callback<ApiResponse<QueryUserInfoVO>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<QueryUserInfoVO>> call, Response<ApiResponse<QueryUserInfoVO>> response){
                                handleResponse(response, callback);
                            }
                            @Override
                            public void onFailure(Call<ApiResponse<QueryUserInfoVO>> call, Throwable t){
                                callback.onFailure("网络异常");
                            }
                        });
    }


    public void searchUser(Long account, UserRepository.UserCallback<ApiResponse<SearchUserVO>> callback){
        userApi.searchUser(account).enqueue(
                new Callback<ApiResponse<SearchUserVO>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<SearchUserVO>> call, Response<ApiResponse<SearchUserVO>> response){
                        handleResponse(response, callback);
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<SearchUserVO>> call, Throwable t){
                        callback.onFailure("网络异常");
                    }
                });
    }

    private <T> void handleResponse(Response<ApiResponse<T>> response, UserRepository.UserCallback<ApiResponse<T>> callback){
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

    public interface UserCallback<T>{

        void onSuccess(T response);

        void onFailure(String error);
    }

    public interface UpdateAvatarCallback {
        void onSuccess(String newAvatarUrl);
        void onError(String errorMsg);
    }

    public interface UpdateNicknameCallback {
        void onSuccess(String newAvatarUrl);
        void onError(String errorMsg);
    }


    /**
     * 🌟 头像直传流水线: 取直传凭证 ➔ 直传对象存储(不经过应用服务器) ➔ 存对象 key
     */
    public void updateAvatar(String localPath, UpdateAvatarCallback callback) {
        File file = new File(localPath);
        if (!file.exists()) {
            if (callback != null) callback.onError("本地图片文件不存在");
            return;
        }
        // 步骤 1: 向后端申请直传凭证 (objectKey + 预签名 PUT 地址)
        userApi.getAvatarUploadCredential().enqueue(new Callback<ApiResponse<OssUploadCredentialVO>>() {
            @Override
            public void onResponse(Call<ApiResponse<OssUploadCredentialVO>> call, Response<ApiResponse<OssUploadCredentialVO>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 1000
                        && response.body().getData() != null) {
                    OssUploadCredentialVO credential = response.body().getData();
                    directUploadToOss(file, credential, localPath, callback);
                } else {
                    if (callback != null) callback.onError("获取上传凭证失败");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<OssUploadCredentialVO>> call, Throwable t) {
                if (callback != null) callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }

    /**
     * 链式步骤 2: 用预签名 URL 把文件字节直接 PUT 到对象存储,
     * 字节流不经过应用服务器, 不占用服务器带宽
     */
    private void directUploadToOss(File file, OssUploadCredentialVO credential,
                                   String localPath, UpdateAvatarCallback callback) {
        // Content-Type 必须与后端签名时声明的 image/jpeg 完全一致, 否则签名校验失败
        RequestBody body = RequestBody.create(MediaType.parse("image/jpeg"), file);
        Request request = new Request.Builder()
                .url(credential.getUploadUrl())
                .put(body)
                .build();
        DIRECT_UPLOAD_CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                response.close();
                if (response.isSuccessful()) {
                    updateAvatarInBackend(localPath, credential.getObjectKey(), callback);
                } else {
                    if (callback != null) callback.onError("头像直传存储失败");
                }
            }
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                if (callback != null) callback.onError("头像上传失败: " + e.getMessage());
            }
        });
    }

    /**
     * 链式步骤 3: 把对象 key 交给后端持久化, 后端返回签名后的可访问 URL
     */
    private void updateAvatarInBackend(String localPath, String avatarObjectKey, UpdateAvatarCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("avatarUrl", avatarObjectKey);
        userApi.updateAvatar(body).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 1000) {
                    // 后端返回签名后的可访问 URL, 本地缓存直接用它
                    String signedAvatarUrl = response.body().getData();
                    String savedSandboxPath = ChatImageUtil.saveAvatarToAppDir(localPath);
                    // 同步覆盖固定路径头像文件, 防止"我的"页/资料详情页读到旧头像
                    ChatImageUtil.saveUserAvatarFixedPath(SessionManager.getUserId(), localPath);
                    SessionManager.saveAvatar(savedSandboxPath, signedAvatarUrl);
                    // 更新全局头像缓存, 覆盖旧 URL
                    AvatarManager.put(SessionManager.getUserId(), signedAvatarUrl);
                    Log.d(TAG, "新头像已安全存入沙盒目录: " + savedSandboxPath);
                    if (callback != null) callback.onSuccess(signedAvatarUrl);
                } else {
                    if (callback != null) callback.onError("数据库更新头像失败");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                if (callback != null) callback.onError("网络错误: " + t.getMessage());
            }
        });
    }

    /**
     * 🌟 修改用户昵称流水线 (更新后端 MySQL + 同步更新本地 SharedPreferences)
     */
    public void updateNickname(String newNickname, UpdateNicknameCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("nickname", newNickname);
        userApi.updateNickname(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 1000) {
                    SessionManager.saveNickname(newNickname);
                    if (callback != null) callback.onSuccess(newNickname);
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "修改昵称失败";
                    if (callback != null) callback.onError(msg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                if (callback != null) callback.onError("网络错误: " + t.getMessage());
            }
        });
    }
}

