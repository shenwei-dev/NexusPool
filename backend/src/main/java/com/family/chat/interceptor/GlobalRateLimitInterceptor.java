package com.family.chat.interceptor;

import com.family.chat.util.GlobalSlidingWindowLimiter;
import com.family.chat.util.RateLimitConfigLoader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.List;

/**
 * 全局接口限流拦截器 (GlobalRateLimitInterceptor)。
 * <p>实现 {@link HandlerInterceptor}。
 * 主要功能是对系统所有接口请求进行全局限流，防止短时间内请求洪峰压垮系统。
 * </p>
 *
 * @author ShenWei
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings("NullableProblems")
public class GlobalRateLimitInterceptor implements HandlerInterceptor {

    /** 全局滑动窗口限流器 */
    private final GlobalSlidingWindowLimiter globalSlidingWindowLimiter;

    /** 全局限流配置加载器，支持动态读取 Redis 配置 */
    private final RateLimitConfigLoader rateLimitConfigLoader;

    /**
     * 请求前拦截方法，执行全局限流逻辑
     *
     * @param request  {@link HttpServletRequest} 请求对象
     * @param response {@link HttpServletResponse} 响应对象
     * @param handler  请求处理对象
     * @return {@code true} 表示允许请求通过，{@code false} 表示被限流拦截
     * @throws Exception 当限流或响应写入异常时抛出
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 动态读取 Redis 配置
        RateLimitConfigLoader.GlobalLimitConfig globalLimitConfig = rateLimitConfigLoader.loadGlobalConfig();

        // 获取ip
        String ip = request.getRemoteAddr();

        // 调用全局限流器
        List<Object> result = globalSlidingWindowLimiter.tryAcquire(ip, globalLimitConfig.getSystemLimit(), globalLimitConfig.getIpLimit(), globalLimitConfig.getWindowMs());

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
        response.setHeader("X-GlobalRateLimit-Remaining", String.valueOf(remaining));

        // 放行请求
        return true;
    }
}
