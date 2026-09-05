package com.ridex.auth.service;

import java.util.UUID;

import com.ridex.auth.dto.request.LoginRequest;
import com.ridex.auth.dto.request.RegisterRequest;
import com.ridex.auth.dto.response.LoginResponse;
import com.ridex.auth.dto.response.RefreshTokenResponse;
import com.ridex.auth.dto.response.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    RefreshTokenResponse refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
    void logoutAll(UUID userId);

}
