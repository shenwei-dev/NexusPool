package com.family.chat.config;

import com.family.chat.interceptor.GlobalRateLimitInterceptor;
import com.family.chat.interceptor.ApiRateLimitInterceptor;
import com.family.chat.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类 (WebConfig)。
 *
 * <p>负责注册全局拦截器, 包括全局限流、接口限流和JWT鉴权。
 * traceId 由 {@link com.family.chat.trace.TraceIdFilter} 在 Servlet 层统一注入。<p/>
 *
 * @author ShenWei
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /** 接口级限流拦截器 */
    private final ApiRateLimitInterceptor apiRateLimitInterceptor;

    /** 全局限流拦截器 */
    private final GlobalRateLimitInterceptor globalRateLimitInterceptor;

    /** JWT 鉴权拦截器 */
    private final JwtInterceptor jwtInterceptor;

    /**
     * 添加拦截器到 Spring MVC 拦截链。
     * 顺序很重要: 先进行全局限流, 接口限流, 最后执行登录态鉴权。
     *
     * @param registry {@link InterceptorRegistry} 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 全局限流
        registry.addInterceptor(globalRateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/health",
                                    "/static/**",
                                    "/api/auth/login",
                                    "/api/auth/register"
                );


        // 接口限流
        registry.addInterceptor(apiRateLimitInterceptor).addPathPatterns("/**");

        // JWT 登录态鉴权: 除登录、注册、刷新令牌外的所有业务接口必须携带合法令牌
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login",
                                     "/api/auth/register",
                                     "/api/auth/refresh"
                );
    }
}
