package com.family.chat.common;

import android.os.Build;
import java.time.Instant;
import java.util.UUID;

// 通用响应体的封装
public class ApiResponse<T> {

    /** 业务状态码，1000 表示成功，非1000表示失败 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 返回数据 */
    private T data;

    /** 响应时间戳（毫秒） */
    private long timestamp;

    /** 链路追踪ID */
    private String traceId;

    /** 构造方法 */
    private ApiResponse() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.timestamp = Instant.now().toEpochMilli();
        }
        this.traceId = UUID.randomUUID().toString();
    }

    private ApiResponse(int code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    private ApiResponse(int code, String message, T data, String traceId) {
        this(code, message, data);
        if (traceId != null && !traceId.isEmpty()) {
            this.traceId = traceId;
        }
    }

    /** Getter */
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    /** toString() */
    @Override
    public String toString() {
        return "ApiResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                ", traceId='" + traceId + '\'' +
                '}';
    }

    /** 静态工厂方法 */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> ApiResponse<T> fail(ResultCode resultCode) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }

    /** 链式调用 */
    public ApiResponse<T> code(int code) {
        this.code = code;
        return this;
    }

    public ApiResponse<T> message(String message) {
        this.message = message;
        return this;
    }

    public ApiResponse<T> data(T data) {
        this.data = data;
        return this;
    }

    public ApiResponse<T> traceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            this.traceId = traceId;
        }
        return this;
    }
}