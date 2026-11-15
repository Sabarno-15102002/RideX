package com.ridex.auth.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ridex.auth.entity.RefreshToken;
import com.ridex.auth.entity.User;
import com.ridex.auth.exception.InvalidRefreshTokenException;
import com.ridex.auth.repository.RefreshTokenRepository;
import com.ridex.auth.service.util.GeneratedRefreshToken;
import com.ridex.auth.service.util.RefreshTokenGenerator;
import com.ridex.auth.service.util.TokenHashUtil;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    private static final long EXPIRATION_MS = 86400000L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenGenerator tokenGenerator;

    @Mock
    private TokenHashUtil tokenHashUtil;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User user;
    private RefreshToken currentToken;

    private UUID userId;
    private UUID tokenId;
    private UUID familyId;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                refreshTokenService,
                "expirationMs",
                EXPIRATION_MS
        );

        userId = UUID.randomUUID();
        tokenId = UUID.randomUUID();
        familyId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .name("John Doe")
                .email("john@example.com")
                .build();

        currentToken = RefreshToken.builder()
                .id(tokenId)
                .userId(userId)
                .tokenHash("old-hash")
                .familyId(familyId)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
    }

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------

    @Test
    void create_shouldGenerateAndPersistRefreshToken() {

        when(tokenGenerator.generate())
                .thenReturn("raw-refresh-token");

        when(tokenHashUtil.hash("raw-refresh-token"))
                .thenReturn("hashed-refresh-token");

        GeneratedRefreshToken result =
                refreshTokenService.create(user);

        assertNotNull(result);
        assertEquals("raw-refresh-token", result.rawToken());

        RefreshToken savedToken = result.entity();

        assertEquals(userId, savedToken.getUserId());
        assertEquals("hashed-refresh-token", savedToken.getTokenHash());
        assertNotNull(savedToken.getFamilyId());
        assertFalse(savedToken.isRevoked());
        assertNotNull(savedToken.getCreatedAt());
        assertNotNull(savedToken.getExpiresAt());

        assertTrue(
                savedToken.getExpiresAt().isAfter(Instant.now())
        );

        verify(tokenGenerator).generate();
        verify(tokenHashUtil).hash("raw-refresh-token");
        verify(refreshTokenRepository).save(savedToken);
    }

    @Test
    void create_shouldStoreOnlyHashedToken() {

        when(tokenGenerator.generate())
                .thenReturn("secret-raw-token");

        when(tokenHashUtil.hash("secret-raw-token"))
                .thenReturn("sha256-hash");

        GeneratedRefreshToken result =
                refreshTokenService.create(user);

        assertEquals(
                "sha256-hash",
                result.entity().getTokenHash()
        );

        assertNotEquals(
                result.rawToken(),
                result.entity().getTokenHash()
        );
    }

    @Test
    void create_shouldGenerateUniqueFamilyId() {

        when(tokenGenerator.generate())
                .thenReturn("token-one", "token-two");

        when(tokenHashUtil.hash(any()))
                .thenReturn("hash-one", "hash-two");

        GeneratedRefreshToken first =
                refreshTokenService.create(user);

        GeneratedRefreshToken second =
                refreshTokenService.create(user);

        assertNotNull(first.entity().getFamilyId());
        assertNotNull(second.entity().getFamilyId());

        assertNotEquals(
                first.entity().getFamilyId(),
                second.entity().getFamilyId()
        );
    }

    // ---------------------------------------------------------
    // ROTATE
    // ---------------------------------------------------------

    @Test
    void rotate_shouldSuccessfullyRotateValidToken() {

        when(refreshTokenRepository.revokeIfActive(tokenId))
                .thenReturn(1);

        when(tokenGenerator.generate())
                .thenReturn("new-raw-token");

        when(tokenHashUtil.hash("new-raw-token"))
                .thenReturn("new-token-hash");

        doAnswer(invocation -> {
            RefreshToken saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        }).when(refreshTokenRepository).save(any(RefreshToken.class));

        GeneratedRefreshToken result =
                refreshTokenService.rotate(currentToken, user);

        assertNotNull(result);
        assertEquals("new-raw-token", result.rawToken());

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken newToken = captor.getValue();

        assertEquals(userId, newToken.getUserId());
        assertEquals("new-token-hash", newToken.getTokenHash());
        assertEquals(familyId, newToken.getFamilyId());
        assertFalse(newToken.isRevoked());

        verify(refreshTokenRepository).revokeIfActive(tokenId);

        verify(refreshTokenRepository).setReplacedBy(
                tokenId,
                newToken.getId()
        );

        verify(tokenGenerator).generate();
        verify(tokenHashUtil).hash("new-raw-token");
    }

    @Test
    void rotate_shouldPreserveTokenFamily() {

        when(refreshTokenRepository.revokeIfActive(tokenId))
                .thenReturn(1);

        when(tokenGenerator.generate())
                .thenReturn("new-token");

        when(tokenHashUtil.hash("new-token"))
                .thenReturn("new-hash");

        doAnswer(invocation -> {
            RefreshToken saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        }).when(refreshTokenRepository).save(any(RefreshToken.class));

        GeneratedRefreshToken result =
                refreshTokenService.rotate(currentToken, user);

        assertEquals(
                currentToken.getFamilyId(),
                result.entity().getFamilyId()
        );
    }

    @Test
    void rotate_shouldRejectExpiredToken() {

        currentToken.setExpiresAt(
                Instant.now().minusSeconds(60)
        );

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(currentToken, user)
        );

        verify(refreshTokenRepository).revokeIfActive(tokenId);

        verify(refreshTokenRepository, never())
                .revokeFamily(any(UUID.class));

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));

        verify(tokenGenerator, never()).generate();
    }

    @Test
    void rotate_shouldRevokeFamilyWhenTokenIsAlreadyRevoked() {

        currentToken.setRevoked(true);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(currentToken, user)
        );

        verify(refreshTokenRepository).revokeFamily(familyId);

        verify(refreshTokenRepository, never())
                .revokeIfActive(any(UUID.class));

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));

        verify(tokenGenerator, never()).generate();
    }

    @Test
    void rotate_shouldRevokeFamilyWhenAtomicRevocationFails() {

        when(refreshTokenRepository.revokeIfActive(tokenId))
                .thenReturn(0);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(currentToken, user)
        );

        verify(refreshTokenRepository).revokeIfActive(tokenId);
        verify(refreshTokenRepository).revokeFamily(familyId);

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));

        verify(tokenGenerator, never()).generate();
    }

    // ---------------------------------------------------------
    // REVOKE
    // ---------------------------------------------------------

    @Test
    void revoke_shouldRevokeExistingRefreshToken() {

        when(tokenHashUtil.hash("raw-token"))
                .thenReturn("hashed-token");

        when(refreshTokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.of(currentToken));

        refreshTokenService.revoke("raw-token");

        verify(tokenHashUtil).hash("raw-token");

        verify(refreshTokenRepository)
                .findByTokenHash("hashed-token");

        verify(refreshTokenRepository)
                .revokeIfActive(tokenId);
    }

    @Test
    void revoke_shouldRejectUnknownRefreshToken() {

        when(tokenHashUtil.hash("unknown-token"))
                .thenReturn("unknown-hash");

        when(refreshTokenRepository.findByTokenHash("unknown-hash"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.revoke("unknown-token")
        );

        verify(refreshTokenRepository, never())
                .revokeIfActive(any(UUID.class));
    }

    // ---------------------------------------------------------
    // REVOKE ALL
    // ---------------------------------------------------------

    @Test
    void revokeAll_shouldRevokeAllTokensForUser() {

        refreshTokenService.revokeAll(userId);

        verify(refreshTokenRepository)
                .revokeAllByUserId(userId);
    }
}
