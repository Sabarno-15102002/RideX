package com.ridex.matching.config;

import com.ridex.matching.dto.AuthenticatedUser;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "01234567890123456789012345678901";

    private static final String WRONG_SECRET = "abcdefghijklmnopqrstuvwxyz123456";

    private SecretKey secretKey;
    private JwtAuthenticationFilter filter;

    private UUID userId;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8));

        filter = new JwtAuthenticationFilter(SECRET);
        userId = UUID.randomUUID();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldAuthenticateValidToken()
            throws Exception {

        String token = generateToken(
                userId.toString(),
                "RIDER",
                Instant.now().plusSeconds(3600),
                secretKey);

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();

        assertThat(authentication.getPrincipal())
                .isInstanceOf(AuthenticatedUser.class);

        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();

        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal.getRole()).isEqualTo("RIDER");

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_RIDER");

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateWhenAuthorizationHeaderIsMissing()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateWhenAuthorizationHeaderIsNotBearer()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("Authorization", "Basic abc123");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateInvalidToken()
            throws Exception {

        String invalidToken = generateToken(
                userId.toString(),
                "RIDER",
                Instant.now().plusSeconds(3600),
                Keys.hmacShaKeyFor(
                        WRONG_SECRET.getBytes(StandardCharsets.UTF_8)));

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer " + invalidToken);

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateExpiredToken()
            throws Exception {

        String expiredToken = generateToken(
                userId.toString(),
                "RIDER",
                Instant.now().minusSeconds(3600),
                secretKey);

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer " + expiredToken);

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();

        verify(chain).doFilter(request, response);
    }

    private String generateToken(
            String subject,
            String role,
            Instant expiration,
            SecretKey key) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }
}
