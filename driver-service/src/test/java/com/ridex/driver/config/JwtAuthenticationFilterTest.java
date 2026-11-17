package com.ridex.driver.config;

import com.ridex.driver.dto.AuthenticatedUser;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "driver-service-test-secret-key-must-be-at-least-32-bytes";

    private SecretKey secretKey;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );
        filter = new JwtAuthenticationFilter(SECRET);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_withoutAuthorizationHeader_shouldContinueUnauthenticated()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilter_withNonBearerHeader_shouldContinueUnauthenticated()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilter_withValidToken_shouldSetAuthentication()
            throws Exception {

        UUID userId = UUID.randomUUID();
        String token = createToken(userId, "DRIVER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        var authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());

        AuthenticatedUser principal =
                (AuthenticatedUser) authentication.getPrincipal();

        assertEquals(userId, principal.getUserId());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_DRIVER")));

        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilter_withInvalidSignature_shouldNotSetAuthentication()
            throws Exception {

        SecretKey anotherKey = Keys.hmacShaKeyFor(
                "another-test-secret-key-that-is-at-least-32-bytes"
                        .getBytes(StandardCharsets.UTF_8)
        );

        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "DRIVER")
                .signWith(anotherKey)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilter_withMalformedToken_shouldNotSetAuthentication()
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
    void doFilter_withInvalidSubjectUuid_shouldNotSetAuthentication()
            throws Exception {

        String token = Jwts.builder()
                .subject("not-a-uuid")
                .claim("role", "DRIVER")
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
