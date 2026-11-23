package com.family.chat.service.impl;

import com.family.chat.enums.UserStatus;
import com.family.chat.enums.PrivacyMode;
import com.family.chat.common.result.ApiResponse;
import com.family.chat.common.result.ResultCode;
import com.family.chat.config.JwtProperties;
import com.family.chat.dto.LoginDTO;
import com.family.chat.netty.SessionManager;
import com.family.chat.request.LoginRequest;
import com.family.chat.request.RefreshRequest;
import com.family.chat.request.RegisterRequest;
import com.family.chat.entity.User;
import com.family.chat.exception.ServiceException;
import com.family.chat.mapper.UserMapper;
import com.family.chat.service.AuthService;
import com.family.chat.util.*;
import com.family.chat.vo.RegisterVO;
import io.jsonwebtoken.Claims;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final JwtProperties jwtProperties;

    private final JwtUtil jwtUtil;

    private final PasswordUtil passwordUtil;

    private final SessionManager sessionManager;

    private final AccountGenerator accountGenerator;

    // 登录
    @Override
    public LoginDTO login(LoginRequest loginRequest) {
        // 查询用户
        User user = userMapper.selectByAccount(loginRequest.getAccount());

        // 如果用户不存在,则抛出异常
        if (user == null) {
            throw new ServiceException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 如果用户被封禁,则抛出异常
        if (UserStatus.BANNED.getCode() == user.getStatus()) {
            throw new ServiceException(ResultCode.USER_BLOCKED);
        }

        // 记录user对象的id(优化性能)
        Long userId = user.getId();

        // 记录user对象的account(优化性能)
        Long account = user.getAccount();

        // 如果用户不存在或者密码不正确,那么就抛出用户名或密码错误的异常
        if (!passwordUtil.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new ServiceException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 生成 AccessToken 和 RefreshToken
        String accessToken = jwtUtil.generateAccessToken(userId, account);
        String refreshToken = jwtUtil.generateRefreshToken(userId, account);

        // 获取刷新token前缀
        String redisKey = jwtProperties.getRefreshTokenPrefix() + userId;

        // 写入刷新token: 重复登录必须覆盖旧值, 否则新签发的 refreshToken 无法通过 refresh 一致性校验
        redisTemplate.opsForValue().set(redisKey, refreshToken, 30, TimeUnit.DAYS);

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccessToken(accessToken);
        loginDTO.setRefreshToken(refreshToken);
        loginDTO.setUserId(userId);
        loginDTO.setAccount(account);
        loginDTO.setNickname(user.getNickname());

        return loginDTO;
    }

    /**
     * Refresh token flow: signature and expiry are validated by JwtUtil, then the token must
     * match the server-side copy stored in Redis (blocks forged or already-rotated tokens).
     * The refresh token itself is rotated on every refresh to limit replay exposure.
     */
    @Override
    public LoginDTO refresh(RefreshRequest refreshRequest) {
        String presentedToken = refreshRequest.getRefreshToken();

        Claims claims = jwtUtil.parseToken(presentedToken);
        Long userId = claims.get("userId", Long.class);
        Long account = claims.get("account", Long.class);
        if (userId == null) {
            throw new ServiceException(ResultCode.INVALID_TOKEN);
        }

        String redisKey = jwtProperties.getRefreshTokenPrefix() + userId;
        Object storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken == null || !presentedToken.equals(storedToken.toString())) {
            log.warn("Refresh token mismatch or expired in Redis, forcing re-login. [userId={}]", userId);
            throw new ServiceException(ResultCode.UNAUTHORIZED);
        }

        String newAccessToken = jwtUtil.generateAccessToken(userId, account);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, account);
        redisTemplate.opsForValue().set(redisKey, newRefreshToken, 30, TimeUnit.DAYS);

        log.info("Access token refreshed successfully. [userId={}]", userId);

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccessToken(newAccessToken);
        loginDTO.setRefreshToken(newRefreshToken);
        loginDTO.setUserId(userId);
        loginDTO.setAccount(account);
        return loginDTO;
    }

    // 注册
    @Override
    public RegisterVO register(RegisterRequest registerRequest) {
        Long allocatedAccount = accountGenerator.generate();
        User user = buildUserEntity(allocatedAccount, registerRequest.getPassword());

        try {
            userMapper.insert(user);
            log.info("Successfully registered new user. [account={}]",
                    allocatedAccount);
            return new RegisterVO(allocatedAccount);
        } catch (DuplicateKeyException e) {
            log.error("Unique key constraint violation on user account insertion.", e);
            throw new ServiceException(ResultCode.REGISTRATION_BUSY_RETRY);
        }
    }

    @Override
    public void logout(String accessToken) {
        Long userId = SecurityUtil.getCurrentUserId();
        log.info("[Logout] User [{}] is initiating logout sequence...", userId);

        if (accessToken != null) {
            String cleanToken = accessToken.replace("Bearer ", "");

            // 1. 计算该 Token 物理生存期限还剩多少毫秒
            long remainingMs = jwtUtil.getRemainingTime(cleanToken);

            if (remainingMs > 0) {
                String blacklistKey = "auth:blacklist:" + cleanToken;

                // 2. 👈 【核心修改二】：写入 Redis 黑名单，生存期直接设为它的剩余寿命（自动消亡，防内存泄露）
                redisTemplate.opsForValue().set(
                        blacklistKey,
                        "1",
                        remainingMs,
                        TimeUnit.MILLISECONDS
                );
                log.info("[Logout] Token blacklisted in Redis. [remaining_ms={}]", remainingMs);
            }
        }

        // Revoke the refresh token as well, otherwise the session could silently resurrect itself
        redisTemplate.delete(jwtProperties.getRefreshTokenPrefix() + userId);

        Channel channel = sessionManager.getChannel(userId);
        if (channel != null && channel.isActive()) {
            log.info("[Logout] Active Netty channel found for user [{}]. Closing channel...", userId);
            channel.close();
        }
    }

    /**
     * 【工厂方法】：专职负责将原始入参组装、初始化为标准的物理 User 实体类
     */
    private User buildUserEntity(Long allocatedAccount, String rawPassword) {
        User user = new User();
        user.setId(UserIdentifierGenerator.nextId());
        user.setAccount(allocatedAccount);
        user.setPasswordHash(passwordUtil.encode(rawPassword));
        user.setNickname("");
        user.setAvatarUrl("/default_avatar.jpg");
        user.setStatus(UserStatus.NORMAL.getCode());
        user.setPrivacyMode(PrivacyMode.PUBLIC.getCode());
        long currentTime = System.currentTimeMillis();
        user.setCreatedAt(currentTime);
        user.setUpdatedAt(currentTime);
        user.setDeleted(false);
        user.setDeletedAt(null);
        return user;
    }
}
