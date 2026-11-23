package com.family.chat.util;

import com.family.chat.common.result.ResultCode;
import com.family.chat.config.JwtProperties;
import com.family.chat.exception.ServiceException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 工具类。
 *
 * <p>负责 AccessToken/RefreshToken 的生成与解析，并将解析异常映射为统一业务异常码。
 */
@Component
@Slf4j
@AllArgsConstructor
public class JwtUtil {

    /** JWT 配置（密钥、有效期等） */
    private final JwtProperties jwtProperties;

    /**
     * 生成访问令牌（AccessToken）。
     *
     * @param userId  用户 ID
     * @param account 账号
     * @return JWT 字符串
     */
    public String generateAccessToken(Long userId, Long account) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("account", account)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpireMs()))
                .signWith(Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    /**
     * 生成刷新令牌（RefreshToken）。
     *
     * @param userId  用户 ID
     * @param account 账号
     * @return JWT 字符串
     */
    public String generateRefreshToken(Long userId, Long account) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("account", account)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpireMs()))
                .signWith(Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    /**
     * 解析 JWT 并返回 Claims。
     *
     * @param token JWT 字符串
     * @return Claims
     * @throws ServiceException 令牌过期或非法
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new ServiceException(ResultCode.UNAUTHORIZED);
        } catch (JwtException e) {
            throw new ServiceException(ResultCode.INVALID_TOKEN);
        }
    }

    /**
     * 从 Token 中解析用户 ID。
     *
     * @param token JWT 字符串
     * @return userId
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 获取 Token 剩余有效时间。
     *
     * @param token JWT 字符串
     * @return 剩余毫秒数；解析失败返回 0
     */
    public long getRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remainingMs, 0);
        } catch (Exception e) {
            return 0;
        }
    }
}
