
package com.ridex.auth.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private UUID userId;

    @BeforeEach
    void setUp() {

        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        userId = UUID.randomUUID();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldContinueChainWhenAuthorizationHeaderIsMissing()
            throws Exception {

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication());

        verifyNoInteractions(jwtProvider);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldContinueChainWhenAuthorizationHeaderIsNotBearer()
            throws Exception {

        request.addHeader("Authorization", "Basic some-token");

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication());

        verifyNoInteractions(jwtProvider);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldAuthenticateUserForValidToken()
            throws Exception {

        request.addHeader("Authorization", "Bearer valid-token");

        Claims claims = mock(Claims.class);

        when(jwtProvider.isValid("valid-token"))
                .thenReturn(true);

        when(jwtProvider.extractClaims("valid-token"))
                .thenReturn(claims);

        when(claims.getSubject())
                .thenReturn(userId.toString());

        when(claims.get("role", String.class))
                .thenReturn("RIDER");

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());

        assertEquals(userId, authentication.getPrincipal());

        assertTrue(
                authentication.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_RIDER")));

        verify(jwtProvider).isValid("valid-token");
        verify(jwtProvider).extractClaims("valid-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateUserForInvalidToken()
            throws Exception {

        request.addHeader("Authorization", "Bearer invalid-token");

        when(jwtProvider.isValid("invalid-token"))
                .thenReturn(false);

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication());

        verify(jwtProvider).isValid("invalid-token");
        verify(jwtProvider, never()).extractClaims(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldClearContextWhenJwtParsingFails()
            throws Exception {

        request.addHeader("Authorization", "Bearer invalid-token");

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userId, null, java.util.List.of()));

        when(jwtProvider.isValid("invalid-token"))
                .thenReturn(true);

        when(jwtProvider.extractClaims("invalid-token"))
                .thenThrow(new JwtException("Invalid JWT"));

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldClearContextWhenSubjectIsNotValidUuid()
            throws Exception {

        request.addHeader("Authorization", "Bearer valid-token");

        Claims claims = mock(Claims.class);

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userId, null, java.util.List.of()));

        when(jwtProvider.isValid("valid-token"))
                .thenReturn(true);

        when(jwtProvider.extractClaims("valid-token"))
                .thenReturn(claims);

        when(claims.getSubject())
                .thenReturn("not-a-valid-uuid");

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldContinueChainWhenBearerTokenIsEmpty()
            throws Exception {

        request.addHeader("Authorization", "Bearer ");

        when(jwtProvider.isValid(""))
                .thenReturn(false);

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication());

        verify(jwtProvider).isValid("");
        verify(filterChain).doFilter(request, response);
    }
}
