package com.ridex.rider.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ridex.rider.dto.AuthenticatedUser;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long";

    private JwtAuthenticationFilter filter;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(SECRET);
        secretKey = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldAuthenticateValidToken()
            throws Exception {

        UUID userId = UUID.randomUUID();
        String token = createToken(userId, "RIDER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication.getPrincipal() instanceof AuthenticatedUser);

        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();

        assertEquals(userId, principal.getUserId());
        assertEquals("RIDER", principal.getRole());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_RIDER")));
    }

    @Test
    void doFilterInternal_shouldContinueWithoutAuthorizationHeader()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilterInternal_shouldIgnoreNonBearerAuthorizationHeader()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc123");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateInvalidToken()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateTokenWithInvalidSubject()
            throws Exception {

        String token = Jwts.builder()
                .subject("not-a-uuid")
                .claim("role", "RIDER")
                .signWith(secretKey)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    private String createToken(UUID userId, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .signWith(secretKey)
                .compact();
    }
}
