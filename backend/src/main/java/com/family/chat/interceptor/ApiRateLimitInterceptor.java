package com.family.chat.interceptor;

import com.family.chat.annotation.RateLimit;
import com.family.chat.util.RateLimitConfigLoader;
import com.family.chat.util.RedisSlidingWindowLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.List;

@Component
@RequiredArgsConstructor
@SuppressWarnings("NullableProblems")
public class ApiRateLimitInterceptor implements HandlerInterceptor {

    /** Redis 滑动窗口限流器 */
    private final RedisSlidingWindowLimiter redisSlidingWindowLimiter;

    /** 接口限流配置加载器，支持动态读取 Redis 配置 */
    private final RateLimitConfigLoader rateLimitConfigLoader;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是Controller的方法请求,那么直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 获取控制层上添加的限流注解@RateLimit
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        // 如果控制层上没有添加限流注解,那么直接放行
        if (rateLimit == null) {
            return true;
        }

        // 生成接口唯一标识 key
        String apiKey = rateLimit.key().isEmpty()
                ? handlerMethod.getBeanType().getSimpleName() + ":" + handlerMethod.getMethod().getName()
                : rateLimit.key();

        // 动态读取 Redis 配置
        RateLimitConfigLoader.ApiLimitConfig apiLimitConfig = rateLimitConfigLoader.loadApiConfig(apiKey);

        // 获取用户id(如果开启用户级限流,那么从请求头中获取用户的id,如果前端没有传,那么就用guest)
        String userId = apiLimitConfig.isPerUser() ? request.getHeader("X-User-Id") : "guest";

        // 获取ip
        String ip = apiLimitConfig.isPerIp() ? request.getRemoteAddr() : "0.0.0.0";

        // 接口限流器
        List<Object> result = redisSlidingWindowLimiter.tryAcquire(userId, ip, apiKey,
                apiLimitConfig.getUserLimit(), apiLimitConfig.getIpLimit(), apiLimitConfig.getApiLimit(), apiLimitConfig.getPeriod());


        // 解析返回结果
        long allowed = Long.parseLong(result.get(0).toString());
        String type = result.get(1).toString();
        long remaining = Long.parseLong(result.get(2).toString());

        // 限流拦截响应
        if (allowed == 0) {
            response.setStatus(429);
            response.getWriter().write("请求过于频繁: " + type + ", 剩余次数: " + remaining);
            return false;
        }

        // 在请求头中携带剩余次数
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        // 放行请求
        return true;
    }
}
