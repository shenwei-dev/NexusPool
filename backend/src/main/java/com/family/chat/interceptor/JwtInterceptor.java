package com.family.chat.interceptor;

import com.family.chat.common.result.ResultCode;
import com.family.chat.exception.ServiceException;
import com.family.chat.util.JwtUtil; // 导入您的 JwtUtil
import com.family.chat.config.JwtProperties; // 导入您的配置类

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    /** MDC key of the current user, indexed in Grafana/Loki for per-user troubleshooting */
    private static final String USER_ID_KEY = "userId";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行浏览器的跨域预检（OPTIONS）请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 1. 获取请求头中的 Authorization Token
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ServiceException(ResultCode.UNAUTHORIZED); // 未登录，抛出401
        }

        String token = authorization.replace("Bearer ", "");

        // 2. 👈 【核心安全防护】：拦截并校验该 Token 是否存在于 Redis 黑名单中！
        String blacklistKey = "auth:blacklist:" + token;
        Boolean isBlacklisted = stringRedisTemplate.hasKey(blacklistKey);

        if (Boolean.TRUE.equals(isBlacklisted)) {
            // Possible token replay detected, only keep a masked token digest in logs to avoid credential leakage
            log.warn("Blocked request with blacklisted token after logout. [tokenDigest={}]", maskToken(token));
            throw new ServiceException(ResultCode.UNAUTHORIZED); // 强制返回 401
        }

        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (Exception e) {
            throw new ServiceException(ResultCode.UNAUTHORIZED);
        }
        // 3. 注入用户维度到日志上下文, Grafana 中可按 userId 直接过滤单个用户的全链路日志
        Long userId = claims.get("userId", Long.class);
        if (userId != null) {
            MDC.put(USER_ID_KEY, String.valueOf(userId));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Tomcat 线程会被复用, MDC 必须随请求结束清理, 防止用户维度串写
        MDC.remove(USER_ID_KEY);
    }

    /**
     * 令牌脱敏：仅保留前 8 位用于问题定位，完整令牌严禁输出到日志
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8) + "***";
    }
}