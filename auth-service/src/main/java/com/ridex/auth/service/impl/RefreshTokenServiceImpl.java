package com.ridex.auth.service.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.auth.entity.RefreshToken;
import com.ridex.auth.entity.User;
import com.ridex.auth.exception.InvalidRefreshTokenException;
import com.ridex.auth.repository.RefreshTokenRepository;
import com.ridex.auth.service.RefreshTokenService;
import com.ridex.auth.service.util.GeneratedRefreshToken;
import com.ridex.auth.service.util.RefreshTokenGenerator;
import com.ridex.auth.service.util.TokenHashUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

        private final RefreshTokenRepository refreshTokenRepository;
        private final RefreshTokenGenerator tokenGenerator;
        private final TokenHashUtil tokenHashUtil;

        @Value("${refresh-token.expiration-ms}")
        private long expirationMs;

        @Override
        @Transactional
        public GeneratedRefreshToken create(User user) {

                String rawToken = tokenGenerator.generate();
                Instant now = Instant.now();

                RefreshToken token = RefreshToken.builder()
                                .userId(user.getId())
                                .tokenHash(tokenHashUtil.hash(rawToken))
                                .familyId(UUID.randomUUID())
                                .expiresAt(now.plusMillis(expirationMs))
                                .revoked(false)
                                .createdAt(now)
                                .build();

                refreshTokenRepository.save(token);

                return new GeneratedRefreshToken(
                                rawToken,
                                token);
        }

        @Override
        @Transactional
        public GeneratedRefreshToken rotate(
                        RefreshToken currentToken,
                        User user) {

                Instant now = Instant.now();

                // 1. Check expiration
                if (currentToken.getExpiresAt().isBefore(now)) {

                        refreshTokenRepository.revokeIfActive(
                                        currentToken.getId());

                        throw new InvalidRefreshTokenException(
                                        "Refresh token has expired");
                }

                // 2. Detect reuse
                if (currentToken.isRevoked()) {

                        refreshTokenRepository.revokeFamily(
                                        currentToken.getFamilyId());

                        throw new InvalidRefreshTokenException(
                                        "Refresh token has already been used");
                }

                // 3. Atomically revoke the current token
                int updatedRows = refreshTokenRepository.revokeIfActive(
                                currentToken.getId());

                if (updatedRows != 1) {

                        refreshTokenRepository.revokeFamily(
                                        currentToken.getFamilyId());

                        throw new InvalidRefreshTokenException(
                                        "Refresh token has already been used");
                }

                // 4. Generate a completely new refresh token
                String newRawToken = tokenGenerator.generate();

                // 5. Store only the hash
                RefreshToken newToken = RefreshToken.builder()
                                .userId(user.getId())
                                .tokenHash(
                                                tokenHashUtil.hash(newRawToken))
                                .familyId(currentToken.getFamilyId())
                                .expiresAt(
                                                now.plusMillis(expirationMs))
                                .revoked(false)
                                .createdAt(now)
                                .build();

                refreshTokenRepository.save(newToken);

                refreshTokenRepository.setReplacedBy(
                        currentToken.getId(),
                        newToken.getId()
                );

                return new GeneratedRefreshToken(
                                newRawToken,
                                newToken);
        }

        @Override
        @Transactional
        public void revoke(String rawRefreshToken) {

                String tokenHash = tokenHashUtil.hash(rawRefreshToken);

                RefreshToken token = refreshTokenRepository
                                .findByTokenHash(tokenHash)
                                .orElseThrow(() -> new InvalidRefreshTokenException(
                                                "Invalid refresh token"));

                refreshTokenRepository.revokeIfActive(
                                token.getId());
        }

        @Override
        @Transactional
        public void revokeAll(UUID userId) {

                refreshTokenRepository.revokeAllByUserId(userId);
        }
}