package com.family.chat.service;

import com.family.chat.common.result.ApiResponse;
import com.family.chat.dto.LoginDTO;
import com.family.chat.request.LoginRequest;
import com.family.chat.request.RefreshRequest;
import com.family.chat.request.RegisterRequest;
import com.family.chat.vo.RegisterVO;

public interface AuthService {

    LoginDTO login(LoginRequest loginRequest);

    RegisterVO register(RegisterRequest registerRequest);

    void logout(String accessToken);

    /**
     * Use a valid refresh token to obtain a new access token (and a rotated refresh token).
     */
    LoginDTO refresh(RefreshRequest refreshRequest);
}
