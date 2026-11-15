package com.ridex.trip.config;

import com.ridex.trip.dto.AuthenticatedUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private static final String SECRET = "ridex-test-secret-key-at-least-32-bytes-long";

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void validBearerToken_shouldPopulateSecurityContext() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder().subject(userId.toString()).claim("role", "RIDER")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new JwtAuthenticationFilter(SECRET).doFilter(request, response, chain);

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertInstanceOf(AuthenticatedUser.class, principal);
        assertEquals(userId, ((AuthenticatedUser) principal).getUserId());
        assertTrue(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RIDER")));
        verify(chain).doFilter(request, response);
    }

    @Test void invalidBearerToken_shouldLeaveAuthenticationEmptyAndContinueChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-valid-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new JwtAuthenticationFilter(SECRET).doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test void missingAuthorizationHeader_shouldContinueWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new JwtAuthenticationFilter(SECRET).doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
