package com.family.chat.interceptor;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.family.chat.api.AuthApi;
import com.family.chat.api.RetrofitClient;
import com.family.chat.common.ApiResponse;
import com.family.chat.common.ResultCode;
import com.family.chat.dto.UserRefreshTokenDTO;
import com.family.chat.network.WebSocketManager;
import com.family.chat.ui.activity.LoginActivity;
import com.family.chat.util.SessionManager;
import com.family.chat.vo.UserLoginVO;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

/**
 * 鉴权拦截器: 注入 Token, 并在访问令牌失效时通过刷新令牌静默续期, 续期失败才跳转登录页。
 *
 * <p>注意后端行为: JwtInterceptor 抛出的 UNAUTHORIZED 会被全局异常处理器包装为
 * HTTP 200 + 业务码 1002/1003 返回, 因此除 HTTP 401 外还必须解析响应体业务码。
 */
public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";

    /** 业务码: Token失效 */
    private static final int CODE_UNAUTHORIZED = 1002;

    /** 业务码: Token格式错误 */
    private static final int CODE_INVALID_TOKEN = 1003;

    /** peekBody 读取上限, 业务响应通常远小于该值 */
    private static final long PEEK_BODY_LIMIT = 4096L;

    /** 认证豁免路径: 不注入 Token, 失效时也不触发刷新 (防止刷新请求自身递归) */
    private static final String PATH_LOGIN = "/api/auth/login";
    private static final String PATH_REGISTER = "/api/auth/register";
    private static final String PATH_REFRESH = "/api/auth/refresh";

    /** 单飞锁: 并发请求同时遭遇令牌失效时, 只允许一个线程真正调用刷新接口 */
    private final Object refreshLock = new Object();

    private final Context context;

    /** 此Handler用于在网络线程中切回Android主线程更新UI */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String path = originalRequest.url().encodedPath();
        String token = SessionManager.getAccessToken();

        Response response;
        if (TextUtils.isEmpty(token) || isAuthExempt(path)) {
            response = chain.proceed(originalRequest);
        } else {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Request authorizedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + cleanToken)
                    .build();
            response = chain.proceed(authorizedRequest);
        }

        if (isUnauthorized(response)) {
            if (!isAuthExempt(path) && !TextUtils.isEmpty(token)) {
                // 静默刷新: 成功则用新令牌重试原请求, 对上层业务完全透明
                String newToken = refreshAccessTokenOnce(token);
                if (newToken != null) {
                    response.close();
                    Request retryRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer " + newToken)
                            .build();
                    return chain.proceed(retryRequest);
                }
            }
            redirectToLogin();
        }
        return response;
    }

    private boolean isAuthExempt(String path) {
        return PATH_LOGIN.equals(path) || PATH_REGISTER.equals(path) || PATH_REFRESH.equals(path);
    }

    /**
     * 单飞静默刷新: 若本地令牌已被并发线程刷新成功则直接复用, 否则同步调用刷新接口。
     * 返回新的访问令牌; 刷新失败返回 null (调用方负责跳转登录页)。
     */
    private String refreshAccessTokenOnce(String failedToken) {
        synchronized (refreshLock) {
            String currentToken = SessionManager.getAccessToken();
            if (!Objects.equals(failedToken, currentToken)) {
                // 其他请求已完成刷新, 直接复用新令牌, 避免重复调用刷新接口
                return TextUtils.isEmpty(currentToken) ? null : currentToken;
            }

            String refreshToken = SessionManager.getRefreshToken();
            if (TextUtils.isEmpty(refreshToken)) {
                Log.w(TAG, "No refresh token available, forcing re-login");
                return null;
            }

            try {
                AuthApi authApi = RetrofitClient.getInstance().create(AuthApi.class);
                Call<ApiResponse<UserLoginVO>> call = authApi.refresh(new UserRefreshTokenDTO(refreshToken));
                retrofit2.Response<ApiResponse<UserLoginVO>> retrofitResponse = call.execute();
                ApiResponse<UserLoginVO> body = retrofitResponse.body();
                if (retrofitResponse.isSuccessful() && body != null
                        && body.getCode() == ResultCode.SUCCESS.getCode()
                        && body.getData() != null
                        && !TextUtils.isEmpty(body.getData().getAccessToken())) {
                    UserLoginVO data = body.getData();
                    SessionManager.saveAccessToken(data.getAccessToken());
                    if (!TextUtils.isEmpty(data.getRefreshToken())) {
                        // 后端每次刷新都会轮换刷新令牌, 客户端必须同步落盘
                        SessionManager.saveRefreshToken(data.getRefreshToken());
                    }
                    Log.i(TAG, "Access token refreshed silently, retrying original request");
                    return data.getAccessToken();
                }
                Log.w(TAG, "Token refresh rejected by server, forcing re-login");
            } catch (Exception e) {
                Log.e(TAG, "Token refresh request failed: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * 判定登录态是否失效: HTTP 401, 或 HTTP 200 且业务码为 1002/1003。
     * 使用 peekBody 读取不影响调用方正常消费响应体。
     */
    private boolean isUnauthorized(Response response) {
        if (response.code() == 401) {
            return true;
        }
        if (!response.isSuccessful() || response.body() == null) {
            return false;
        }
        try {
            String preview = response.peekBody(PEEK_BODY_LIMIT).string();
            if (TextUtils.isEmpty(preview) || preview.charAt(0) != '{') {
                return false;
            }
            int code = new JSONObject(preview).optInt("code", 0);
            return code == CODE_UNAUTHORIZED || code == CODE_INVALID_TOKEN;
        } catch (Exception e) {
            // 非 JSON 响应或解析失败不视为鉴权问题
            Log.w(TAG, "解析响应业务码失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用 Handler 切回 Android 主线程, 安全执行 UI 弹窗和 Activity 销毁跳转
     */
    private void redirectToLogin() {
        mainHandler.post(() -> {
            Toast.makeText(context, "您的登录状态已失效，请重新登录", Toast.LENGTH_SHORT).show();
            WebSocketManager.getInstance().disconnect();
            SessionManager.clearSession();
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        });
    }
}
