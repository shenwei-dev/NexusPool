package com.family.chat.api;

import com.family.chat.BuildConfig;
import com.family.chat.ChatApplication;
import com.family.chat.config.AppConfig;
import com.family.chat.interceptor.AuthInterceptor;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.protobuf.ProtoConverterFactory;

/**
 * Retrofit 客户端统一出口。
 *
 * <p>规约要点: 单例使用静态内部类持有者模式保证线程安全与懒加载;
 * HTTP 日志仅在 Debug 包输出且降级为 HEADERS, 避免 Release 包泄漏报文与拖慢性能。
 */
public final class RetrofitClient {

    /** OkHttp 磁盘缓存大小: 10MB */
    private static final long HTTP_CACHE_SIZE = 10L * 1024 * 1024;

    private RetrofitClient() {
    }

    public static Retrofit getInstance() {
        return Holder.RETROFIT;
    }

    private static final class Holder {
        private static final Retrofit RETROFIT = buildRetrofit();
    }

    private static Retrofit buildRetrofit() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(ChatApplication.getContext()))
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                // 在 Android 手机本地分配一个 10MB 的物理缓存目录
                .cache(new Cache(new File(ChatApplication.getContext().getCacheDir(), "http_cache"), HTTP_CACHE_SIZE))
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES));

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            clientBuilder.addInterceptor(logging);
        }

        return new Retrofit.Builder()
                .baseUrl(AppConfig.HTTP_BASE_URL)
                .client(clientBuilder.build())
                .addConverterFactory(ProtoConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
