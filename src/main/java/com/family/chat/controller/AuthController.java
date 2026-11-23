package com.family.chat.controller;

import com.family.chat.common.result.ApiResponse;
import com.family.chat.common.result.ResultCode;
import com.family.chat.dto.LoginDTO;
import com.family.chat.request.LoginRequest;
import com.family.chat.request.RefreshRequest;
import com.family.chat.request.RegisterRequest;
import com.family.chat.service.AuthService;
import com.family.chat.vo.LoginVO;
import com.family.chat.vo.RegisterVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关接口。
 *
 * <p>提供注册、登录、刷新令牌与退出登录能力。
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    /** 认证服务 */
    private final AuthService authService;

    /**
     * 用户注册。
     *
     * @param registerRequest 注册参数
     * @return 注册结果
     */
    @PostMapping("/register")
    public ApiResponse<RegisterVO> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterVO registerVO = authService.register(registerRequest);
        return ApiResponse.success(registerVO);
    }

    /**
     * 用户登录。
     *
     * @param loginRequest 登录参数
     * @return 登录结果（含 token）
     */
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginDTO loginDTO = authService.login(loginRequest);
        LoginVO loginVO = new LoginVO();
        BeanUtils.copyProperties(loginDTO, loginVO);
        return ApiResponse.success(loginVO);
    }

    /**
     * 刷新访问令牌。
     *
     * @param refreshRequest 刷新参数
     * @return 新的登录结果（含 token）
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginVO> refresh(@Valid @RequestBody RefreshRequest refreshRequest) {
        LoginDTO loginDTO = authService.refresh(refreshRequest);
        LoginVO loginVO = new LoginVO();
        BeanUtils.copyProperties(loginDTO, loginVO);
        return ApiResponse.success(loginVO);
    }

    /**
     * 退出登录（使 AccessToken 失效）。
     *
     * @param request HTTP 请求（读取 Authorization 头）
     * @return 处理结果
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ApiResponse.fail(ResultCode.UNAUTHORIZED);
        }
        String accessToken = authorization.substring(7);
        authService.logout(accessToken);
        return ApiResponse.success();
    }
}
