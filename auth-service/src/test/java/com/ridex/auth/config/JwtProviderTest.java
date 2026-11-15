package com.ridex.auth.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ridex.auth.entity.User;
import com.ridex.auth.utilities.UserRole;
import com.ridex.auth.utilities.UserStatus;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

class JwtProviderTest {

    private static final String SECRET = "ridex-test-secret-key-must-be-at-least-32-bytes-long";

    private static final long EXPIRATION_MS = 3600000;

    private JwtProvider jwtProvider;

    private User user;

    @BeforeEach
    void setUp() {

        jwtProvider = new JwtProvider(SECRET, EXPIRATION_MS);

        user = User.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .email("john@example.com")
                .phone("9876543210")
                .passwordHash("encoded-password")
                .role(UserRole.RIDER)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void generateToken_shouldGenerateValidToken() {

        String token = jwtProvider.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtProvider.isValid(token));
    }

    @Test
    void generateToken_shouldContainCorrectUserId() {

        String token = jwtProvider.generateToken(user);

        Claims claims = jwtProvider.extractClaims(token);

        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals(user.getId(), jwtProvider.extractUserId(token));
    }

    @Test
    void generateToken_shouldContainCorrectEmailClaim() {

        String token = jwtProvider.generateToken(user);

        Claims claims = jwtProvider.extractClaims(token);

        assertEquals(user.getEmail(), claims.get("email", String.class));
    }

    @Test
    void generateToken_shouldContainCorrectRoleClaim() {

        String token = jwtProvider.generateToken(user);

        Claims claims = jwtProvider.extractClaims(token);

        assertEquals(
                user.getRole().name(),
                claims.get("role", String.class));
    }

    @Test
    void generateToken_shouldContainIssuedAtAndExpiration() {

        String token = jwtProvider.generateToken(user);

        Claims claims = jwtProvider.extractClaims(token);

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        long issuedAtMillis = claims.getIssuedAt().getTime();
        long expirationMillis = claims.getExpiration().getTime();

        // Issued-at must be a valid timestamp.
        assertTrue(issuedAtMillis > 0);

        // Expiration must be after issued-at.
        assertTrue(expirationMillis > issuedAtMillis);

        // Verify configured token lifetime.
        assertEquals(
                EXPIRATION_MS,
                expirationMillis - issuedAtMillis);
    }

    @Test
    void isValid_shouldReturnTrueForValidToken() {

        String token = jwtProvider.generateToken(user);

        assertTrue(jwtProvider.isValid(token));
    }

    @Test
    void isValid_shouldReturnFalseForExpiredToken() {

        SecretKey key = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8));

        String expiredToken = Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();

        assertFalse(jwtProvider.isValid(expiredToken));
    }

    @Test
    void isValid_shouldReturnFalseForTokenSignedWithDifferentSecret() {

        String differentSecret = "another-test-secret-key-must-be-at-least-32-bytes-long";

        SecretKey differentKey = Keys.hmacShaKeyFor(
                differentSecret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(Date.from(
                        Instant.now().plusMillis(EXPIRATION_MS)))
                .signWith(differentKey)
                .compact();

        assertFalse(jwtProvider.isValid(token));
    }

    @Test
    void isValid_shouldReturnFalseForMalformedToken() {

        assertFalse(jwtProvider.isValid("this-is-not-a-jwt"));
    }

    @Test
    void isValid_shouldReturnFalseForEmptyToken() {

        assertFalse(jwtProvider.isValid(""));
    }

    @Test
    void isValid_shouldReturnFalseForNullToken() {

        assertFalse(jwtProvider.isValid(null));
    }

    @Test
    void extractUserId_shouldReturnUuidFromSubject() {

        String token = jwtProvider.generateToken(user);

        UUID extractedUserId = jwtProvider.extractUserId(token);

        assertEquals(user.getId(), extractedUserId);
    }

    @Test
    void getExpirationMs_shouldReturnConfiguredExpiration() {

        assertEquals(EXPIRATION_MS, jwtProvider.getExpirationMs());
    }
}
