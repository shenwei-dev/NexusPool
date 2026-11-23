package com.family.chat.ai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 大模型网关 (LlmGateway)。
 *
 * <p>对标阿里网关化调用实践: 应用侧不裸调三方 API, 统一经网关收口,
 * 提供请求组装、失败留痕 (含网关真实错误体)、网络异常单次重试。
 *
 * @author ShenWei
 */
@Slf4j
@Component
public class LlmGateway {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /** 网络层瞬时故障 (连接重置/超时) 的兜底重试次数 */
    private static final int MAX_RETRY = 1;

    private final OkHttpClient okHttpClient;

    @Value("${ai.model}")
    private String aiModel;

    @Value("${ai.api-key}")
    private String aiApiKey;

    @Value("${ai.base-url}")
    private String aiBaseUrl;

    public LlmGateway(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    /**
     * 单轮补全请求 (OpenAI 兼容协议)。
     *
     * @param messages 完整对话上下文
     * @param tools    工具声明, 纯文本轮次传 null
     * @return 模型返回的 message 对象; 网关不可用时返回 null 由编排层兜底
     */
    public JSONObject chat(JSONArray messages, JSONArray tools) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", aiModel);
        requestBody.put("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools);
        }

        IOException lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
            try {
                return doCall(requestBody);
            } catch (IOException e) {
                lastException = e;
                log.warn("LLM gateway network error, will retry. [attempt={}]", attempt, e);
            }
        }
        log.error("LLM gateway unreachable after retries.", lastException);
        return null;
    }

    private JSONObject doCall(JSONObject requestBody) throws IOException {
        Request request = new Request.Builder()
                .url(aiBaseUrl)
                .header("Authorization", "Bearer " + aiApiKey)
                .post(RequestBody.create(requestBody.toJSONString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                // Keep the real gateway error body for fast troubleshooting in Grafana/Loki
                log.error("LLM gateway request failed. [httpStatus={}, errorBody={}]", response.code(), responseBody);
                return null;
            }
            return JSONObject.parseObject(responseBody)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message");
        }
    }
}
