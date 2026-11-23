package com.family.chat.interceptor;

import com.family.chat.common.result.ResultCode;
import com.family.chat.exception.ServiceException;
import com.family.chat.util.JwtUtil;
import com.family.chat.util.SecurityUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements Filter {

    private final JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 获取请求对象
        HttpServletRequest http = (HttpServletRequest) request;

        // 从请求头中取出token
        String token = http.getHeader("Authorization");

        // 如果token不为空,且解析有效
        if (token != null) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            try {
                if (jwtUtil.parseToken(token) != null) {
                    // 从token中取出用户ID
                    Long userId = jwtUtil.getUserId(token);

                    // 将当前用户ID设置进线程中
                    SecurityUtil.setCurrentUserId(userId);
                }
            } catch (Exception e) {
                // 令牌无效(过期/格式错误)时不设置用户上下文, 由鉴权拦截器统一拦截, 避免过滤器层抛出未处理异常
                log.warn("Invalid token in request, continuing as anonymous. [reason={}]", e.getMessage());
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityUtil.clear();
        }
    }
}
