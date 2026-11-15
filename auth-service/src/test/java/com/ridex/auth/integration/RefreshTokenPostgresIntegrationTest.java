
package com.ridex.auth.integration;

import com.ridex.auth.entity.RefreshToken;
import com.ridex.auth.entity.User;
import com.ridex.auth.exception.InvalidRefreshTokenException;
import com.ridex.auth.repository.RefreshTokenRepository;
import com.ridex.auth.repository.UserRepository;
import com.ridex.auth.service.RefreshTokenService;
import com.ridex.auth.service.util.GeneratedRefreshToken;
import com.ridex.auth.service.util.TokenHashUtil;
import com.ridex.auth.utilities.UserRole;
import com.ridex.auth.utilities.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false",
                "outbox.publisher.fixed-delay-ms=3600000",
                "jwt.secret=integration-test-secret-key-must-be-at-least-32-bytes",
                "jwt.expiration-ms=900000",
                "refresh-token.expiration-ms=2592000000"
})
class RefreshTokenPostgresIntegrationTest {

        @Container
        static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("refresh_test")
                        .withUsername("test")
                        .withPassword("test");

        @DynamicPropertySource
        static void databaseProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Autowired
        private RefreshTokenService refreshTokenService;

        @Autowired
        private RefreshTokenRepository refreshTokenRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TokenHashUtil tokenHashUtil;

        private User user;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @BeforeEach
        void setUp() {
                jdbcTemplate.update(
                                "UPDATE refresh_tokens SET replaced_by = NULL");

                refreshTokenRepository.deleteAllInBatch();
                userRepository.deleteAllInBatch();
                refreshTokenRepository.deleteAll();
                userRepository.deleteAll();

                Instant now = Instant.now();

                user = User.builder()
                                .id(UUID.randomUUID())
                                .name("Integration Rider")
                                .email("refresh-" + UUID.randomUUID() + "@test.com")
                                .phone(null)
                                .passwordHash("encoded-password")
                                .role(UserRole.RIDER)
                                .status(UserStatus.ACTIVE)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();

                user = userRepository.saveAndFlush(user);
        }

        private RefreshToken createSiblingToken(UUID familyId) {
                Instant now = Instant.now();

                RefreshToken token = RefreshToken.builder()
                                .userId(user.getId())
                                .tokenHash(tokenHashUtil.hash(UUID.randomUUID().toString()))
                                .familyId(familyId)
                                .expiresAt(now.plusSeconds(3600))
                                .revoked(false)
                                .createdAt(now)
                                .build();

                return refreshTokenRepository.saveAndFlush(token);
        }

        private List<RefreshToken> familyTokens(UUID familyId) {
                return refreshTokenRepository.findAll()
                                .stream()
                                .filter(token -> token.getFamilyId().equals(familyId))
                                .toList();
        }

        @Test
        void create_shouldPersistOnlyTokenHash() {
                GeneratedRefreshToken generated = refreshTokenService.create(user);

                RefreshToken saved = refreshTokenRepository
                                .findByTokenHash(tokenHashUtil.hash(generated.rawToken()))
                                .orElseThrow();

                assertNotNull(saved.getId());
                assertEquals(user.getId(), saved.getUserId());
                assertNotEquals(generated.rawToken(), saved.getTokenHash());
                assertEquals(
                                tokenHashUtil.hash(generated.rawToken()),
                                saved.getTokenHash());
                assertFalse(saved.isRevoked());
                assertNotNull(saved.getFamilyId());
                assertNotNull(saved.getExpiresAt());
        }

        @Test
        void create_shouldGenerateDifferentFamiliesForDifferentSessions() {
                GeneratedRefreshToken first = refreshTokenService.create(user);
                GeneratedRefreshToken second = refreshTokenService.create(user);

                assertNotEquals(
                                first.entity().getFamilyId(),
                                second.entity().getFamilyId());
        }

        @Test
        void rotate_shouldRevokeOldTokenAndPersistReplacement() {
                GeneratedRefreshToken original = refreshTokenService.create(user);

                RefreshToken current = refreshTokenRepository
                                .findByTokenHash(tokenHashUtil.hash(original.rawToken()))
                                .orElseThrow();

                GeneratedRefreshToken replacement = refreshTokenService.rotate(current, user);

                RefreshToken oldFromDatabase = refreshTokenRepository.findById(current.getId())
                                .orElseThrow();

                RefreshToken newFromDatabase = refreshTokenRepository
                                .findByTokenHash(
                                                tokenHashUtil.hash(replacement.rawToken()))
                                .orElseThrow();

                assertTrue(oldFromDatabase.isRevoked());
                assertEquals(
                                newFromDatabase.getId(),
                                oldFromDatabase.getReplacedBy());
                assertEquals(
                                oldFromDatabase.getFamilyId(),
                                newFromDatabase.getFamilyId());
                assertFalse(newFromDatabase.isRevoked());
                assertNotEquals(
                                replacement.rawToken(),
                                newFromDatabase.getTokenHash());
        }

        @Test
        void rotate_shouldPreserveUserIdAndFamilyId() {
                GeneratedRefreshToken original = refreshTokenService.create(user);

                RefreshToken current = refreshTokenRepository
                                .findByTokenHash(tokenHashUtil.hash(original.rawToken()))
                                .orElseThrow();

                GeneratedRefreshToken replacement = refreshTokenService.rotate(current, user);

                assertEquals(user.getId(), replacement.entity().getUserId());
                assertEquals(
                                current.getFamilyId(),
                                replacement.entity().getFamilyId());
        }

        @Test
        void revoke_shouldRevokeTokenInPostgres() {
                GeneratedRefreshToken generated = refreshTokenService.create(user);

                refreshTokenService.revoke(generated.rawToken());

                RefreshToken saved = refreshTokenRepository
                                .findByTokenHash(tokenHashUtil.hash(generated.rawToken()))
                                .orElseThrow();

                assertTrue(saved.isRevoked());
        }

        @Test
        void revokeAll_shouldRevokeAllActiveUserTokens() {
                GeneratedRefreshToken first = refreshTokenService.create(user);

                GeneratedRefreshToken second = refreshTokenService.create(user);

                refreshTokenService.revokeAll(user.getId());

                RefreshToken savedFirst = refreshTokenRepository
                                .findByTokenHash(tokenHashUtil.hash(first.rawToken()))
                                .orElseThrow();

                RefreshToken savedSecond = refreshTokenRepository
                                .findByTokenHash(tokenHashUtil.hash(second.rawToken()))
                                .orElseThrow();

                assertTrue(savedFirst.isRevoked());
                assertTrue(savedSecond.isRevoked());
        }

        @Test
        void rotate_shouldRevokeEntireFamilyWhenRevokedTokenIsReused() {
                GeneratedRefreshToken original = refreshTokenService.create(user);

                RefreshToken oldToken = refreshTokenRepository
                                .findByTokenHash(tokenHashUtil.hash(original.rawToken()))
                                .orElseThrow();

                createSiblingToken(oldToken.getFamilyId());

                refreshTokenService.revoke(original.rawToken());

                RefreshToken revokedToken = refreshTokenRepository
                                .findById(oldToken.getId())
                                .orElseThrow();

                assertThrows(
                                InvalidRefreshTokenException.class,
                                () -> refreshTokenService.rotate(revokedToken, user));

                List<RefreshToken> family = familyTokens(oldToken.getFamilyId());

                assertTrue(
                                family.stream().allMatch(RefreshToken::isRevoked),
                                "Every token in the reused token's family must be revoked");
        }

        @Test
        void rotate_shouldRevokeFamilyWhenAtomicRevocationFindsNoActiveRow() {
                GeneratedRefreshToken original = refreshTokenService.create(user);

                RefreshToken staleToken = refreshTokenRepository
                                .findByTokenHash(tokenHashUtil.hash(original.rawToken()))
                                .orElseThrow();

                createSiblingToken(staleToken.getFamilyId());

                // Revoke the database row after loading staleToken.
                // staleToken still has revoked=false in memory.
                refreshTokenService.revoke(original.rawToken());

                assertThrows(
                                InvalidRefreshTokenException.class,
                                () -> refreshTokenService.rotate(staleToken, user));

                List<RefreshToken> family = familyTokens(staleToken.getFamilyId());

                assertTrue(
                                family.stream().allMatch(RefreshToken::isRevoked),
                                "A failed atomic revoke must revoke the whole family");
        }
}
