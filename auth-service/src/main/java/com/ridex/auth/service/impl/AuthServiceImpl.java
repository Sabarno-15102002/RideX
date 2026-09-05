package com.ridex.auth.service.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ridex.auth.config.JwtProvider;
import com.ridex.auth.dto.request.LoginRequest;
import com.ridex.auth.dto.request.RegisterRequest;
import com.ridex.auth.dto.response.LoginResponse;
import com.ridex.auth.dto.response.RefreshTokenResponse;
import com.ridex.auth.dto.response.RegisterResponse;
import com.ridex.auth.entity.User;
import com.ridex.auth.event.UserRegisteredEvent;
import com.ridex.auth.exception.InvalidCredentialsException;
import com.ridex.auth.exception.InvalidRefreshTokenException;
import com.ridex.auth.exception.ResourceAlreadyExistsException;
import com.ridex.auth.repository.RefreshTokenRepository;
import com.ridex.auth.repository.UserRepository;
import com.ridex.auth.service.AuthService;
import com.ridex.auth.service.RefreshTokenService;
import com.ridex.auth.service.util.GeneratedRefreshToken;
import com.ridex.auth.service.util.OutboxEventService;
import com.ridex.auth.service.util.TokenHashUtil;
import com.ridex.auth.utilities.UserRole;
import com.ridex.auth.utilities.UserStatus;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

        private final UserRepository userRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final OutboxEventService outboxEventService;

        private final PasswordEncoder passwordEncoder;
        private final JwtProvider jwtService;
        private final RefreshTokenService refreshTokenService;
        private final TokenHashUtil tokenHashUtil;

        @Transactional
        public RegisterResponse register(RegisterRequest request) {

                if (userRepository.existsByEmail(request.email())) {
                        throw new ResourceAlreadyExistsException(
                                        "Email is already registered");
                }

                if (request.phone() != null &&
                                userRepository.existsByPhone(request.phone())) {

                        throw new ResourceAlreadyExistsException(
                                        "Phone number is already registered");
                }

                User user = User.builder()
                                .id(UUID.randomUUID())
                                .name(request.name())
                                .email(request.email())
                                .phone(request.phone())
                                .passwordHash(
                                                passwordEncoder.encode(
                                                                request.password()))
                                .role(UserRole.RIDER)
                                .status(UserStatus.ACTIVE)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                userRepository.save(user);

                UserRegisteredEvent event = new UserRegisteredEvent(
                                UUID.randomUUID(),
                                user.getId(),
                                request.name(),
                                user.getRole().name(),
                                Instant.now());

                outboxEventService.saveUserRegisteredEvent(event);
                return new RegisterResponse(
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                user.getRole());
        }

        @Transactional
        public LoginResponse login(LoginRequest request) {

                User user = userRepository
                                .findByEmail(request.email())
                                .orElseThrow(() -> new InvalidCredentialsException(
                                                "Invalid email or password"));

                if (!passwordEncoder.matches(
                                request.password(),
                                user.getPasswordHash())) {

                        throw new InvalidCredentialsException(
                                        "Invalid email or password");
                }

                if (user.getStatus() != UserStatus.ACTIVE) {

                        throw new InvalidCredentialsException(
                                        "User account is not active");
                }

                String accessToken = jwtService.generateToken(user);

                GeneratedRefreshToken refreshToken = refreshTokenService.create(user);

                return new LoginResponse(
                                accessToken,
                                refreshToken.rawToken(),
                                "Bearer",
                                jwtService.getExpirationMs(),
                                user.getId(),
                                user.getRole());
        }

        @Transactional
        public RefreshTokenResponse refresh(
                        String rawRefreshToken) {

                String tokenHash = tokenHashUtil.hash(rawRefreshToken);

                var currentToken = refreshTokenRepository
                                .findByTokenHash(tokenHash)
                                .orElseThrow(() -> new InvalidRefreshTokenException(
                                                "Invalid refresh token"));

                User user = userRepository
                                .findById(currentToken.getUserId())
                                .orElseThrow(() -> new InvalidRefreshTokenException(
                                                "User associated with token not found"));

                if (user.getStatus() != UserStatus.ACTIVE) {

                        throw new InvalidRefreshTokenException(
                                        "User account is not active");
                }

                GeneratedRefreshToken rotatedToken = refreshTokenService.rotate(
                                currentToken,
                                user);

                String newAccessToken = jwtService.generateToken(user);

                return new RefreshTokenResponse(
                                newAccessToken,
                                rotatedToken.rawToken(),
                                "Bearer",
                                jwtService.getExpirationMs(),
                                user.getId(),
                                user.getRole());
        }

        @Override
        @Transactional
        public void logout(String rawRefreshToken) {

                refreshTokenService.revoke(
                                rawRefreshToken);
        }

        @Override
        @Transactional
        public void logoutAll(UUID userId) {

                refreshTokenService.revokeAll(userId);
        }
}