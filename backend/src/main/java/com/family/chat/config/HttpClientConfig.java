package com.family.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

@Configuration
public class HttpClientConfig {

    /**
     * 配置全局唯一的 OkHttpClient 单例
     * 针对大模型进行超长读取超时调优，防止深层推理时因超时断开
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS) // 1. 连接超时：15秒（建立连接要快）
                .writeTimeout(15, TimeUnit.SECONDS)   // 2. 写入超时：15秒

                // 3. 👈 【核心优化】：读取超时设为 120 秒（2分钟），完美应对复杂问题及大模型长文本推理！
                .readTimeout(120, TimeUnit.SECONDS)

                // 4. 配置连接复用池，最大空闲连接 10 个，保持 5 分钟
                .connectionPool(new okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build();
    }
}