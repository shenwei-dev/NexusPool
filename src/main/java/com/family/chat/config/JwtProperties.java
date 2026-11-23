package com.family.chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性类。
 *
 * <p>用于从配置文件中读取与 JWT 相关的参数，
 * 包括密钥、有效期和刷新策略。
 */
@Component
@ConfigurationProperties(prefix = "security.jwt")
@Getter
@Setter
public class JwtProperties {

    /** JWT 密钥 */
    private String secretKey;

    /** 访问 Token 有效期（单位：毫秒） */
    private long accessTokenExpireMs;

    /** 刷新 Token 有效期（单位：毫秒） */
    private long refreshTokenExpireMs;

    /** 刷新 Token 前缀 */
    private String refreshTokenPrefix;

    /** Device Redis 前缀 */
    private String devicePrefix;
}
