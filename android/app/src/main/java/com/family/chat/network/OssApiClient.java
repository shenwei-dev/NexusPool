package com.family.chat.network;

import android.content.Context;
import android.util.Log;

import com.family.chat.config.AppConfig;
import com.family.chat.interceptor.AuthInterceptor;
import com.family.chat.vo.ImageUploadCredentialVO;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 图片消息直传客户端。
 *
 * <p>对标阿里云 OSS 客户端直传方案: 先向后端申请直传凭证
 * (objectKey + 短时效预签名 PUT 地址), 再用裸 OkHttpClient 把文件字节
 * 直接 PUT 到对象存储, 图片字节流不经过应用服务器, 不占用服务器带宽。
 *
 * <p>性能要点: 使用 {@code RequestBody.create(MediaType, File)} 由 OkHttp 内部
 * 分段流式读取文件, 避免把整个图片先加载进内存; 网络请求由 OkHttp 自有调度线程池执行,
 * 不在业务代码中散落 new Thread。
 */
public class OssApiClient {

    private static final String TAG = "OssApiClient";

    /** 后端直传凭证接口路径, 对应 OssController GET /api/oss/image/presign */
    private static final String PRESIGN_PATH = "/api/oss/image/presign?suffix=";

    /** 业务成功码 */
    private static final int CODE_SUCCESS = 1000;

    /** 申请凭证走业务客户端 (携带 Token) */
    private final OkHttpClient okHttpClient;

    /** 直传对象存储走裸客户端: 不携带业务 Token/拦截器 */
    private final OkHttpClient directUploadClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    public interface UploadCallback {
        /**
         * @param objectKey   对象 key, 作为图片消息内容持久化到服务端
         * @param downloadUrl 签名后的 GET 地址, 供发送方本地记录兜底展示
         */
        void onSuccess(String objectKey, String downloadUrl);
        void onError(String errorMsg);
    }

    public OssApiClient(Context context) {
        this.okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(context))
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 根据本地物理路径直传图片文件: 取凭证 ➔ PUT 直传对象存储
     */
    public void uploadImage(String localPath, UploadCallback callback) {
        if (localPath == null || localPath.isEmpty()) {
            Log.e(TAG, "上传失败: 本地物理路径为空");
            if (callback != null) callback.onError("本地物理路径为空");
            return;
        }

        File file = new File(localPath);
        if (!file.exists() || file.length() == 0) {
            Log.e(TAG, "上传失败: 本地文件不存在或为空 -> " + localPath);
            if (callback != null) callback.onError("本地文件不存在");
            return;
        }

        fetchCredentialAndUpload(file, callback);
    }

    /**
     * 步骤 1: 向后端申请直传凭证
     */
    private void fetchCredentialAndUpload(File file, UploadCallback callback) {
        String suffix = resolveSuffix(file.getName());
        Request request = new Request.Builder()
                .url(AppConfig.HTTP_BASE_URL + PRESIGN_PATH + suffix)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "申请直传凭证失败, 网络无法连接或超时: " + e.getMessage());
                if (callback != null) callback.onError("申请直传凭证失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                String responseBody = body != null ? body.string() : "";
                if (body != null) {
                    body.close();
                }

                if (!response.isSuccessful()) {
                    Log.e(TAG, "申请凭证服务器错误码: " + response.code());
                    if (callback != null) callback.onError("服务器错误码: " + response.code());
                    return;
                }

                try {
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    int code = jsonObject.get("code").getAsInt();
                    if (code != CODE_SUCCESS) {
                        String message = jsonObject.has("message") ? jsonObject.get("message").getAsString() : "申请凭证失败";
                        Log.e(TAG, "服务器返回失败消息: " + message);
                        if (callback != null) callback.onError(message);
                        return;
                    }
                    JsonObject data = jsonObject.getAsJsonObject("data");
                    ImageUploadCredentialVO credential = new ImageUploadCredentialVO();
                    credential.setObjectKey(data.get("objectKey").getAsString());
                    credential.setUploadUrl(data.get("uploadUrl").getAsString());
                    credential.setDownloadUrl(data.get("downloadUrl").getAsString());
                    directUpload(file, credential, suffix, callback);
                } catch (Exception e) {
                    Log.e(TAG, "解析凭证 JSON 失败", e);
                    if (callback != null) callback.onError("解析凭证响应失败");
                }
            }
        });
    }

    /**
     * 步骤 2: 用预签名 URL 把文件字节直接 PUT 到对象存储
     */
    private void directUpload(File file, ImageUploadCredentialVO credential, String suffix, UploadCallback callback) {
        // Content-Type 必须与后端签名时声明的完全一致, 否则对象存储签名校验失败
        String contentType = ".png".equals(suffix) ? "image/png" : "image/jpeg";
        RequestBody fileBody = RequestBody.create(MediaType.parse(contentType), file);
        Request request = new Request.Builder()
                .url(credential.getUploadUrl())
                .put(fileBody)
                .build();

        directUploadClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "图片直传失败, 网络无法连接或超时: " + e.getMessage());
                if (callback != null) callback.onError("图片直传失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                ResponseBody body = response.body();
                if (body != null) {
                    body.close();
                }
                if (response.isSuccessful()) {
                    Log.d(TAG, "图片直传成功: " + credential.getObjectKey());
                    if (callback != null) callback.onSuccess(credential.getObjectKey(), credential.getDownloadUrl());
                } else {
                    Log.e(TAG, "图片直传存储失败, 错误码: " + response.code());
                    if (callback != null) callback.onError("图片直传存储失败, 错误码: " + response.code());
                }
            }
        });
    }

    /**
     * 归一化文件后缀, 与后端签名声明的 Content-Type 保持一致
     */
    private String resolveSuffix(String fileName) {
        if (fileName != null && fileName.toLowerCase().endsWith(".png")) {
            return ".png";
        }
        return ".jpg";
    }
}
