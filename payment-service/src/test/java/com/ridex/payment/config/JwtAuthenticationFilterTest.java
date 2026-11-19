
package com.ridex.payment.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ridex.payment.dto.AuthenticatedUser;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtAuthenticationFilterTest {

        private static final String SECRET = "this-is-a-test-secret-key-at-least-32-bytes-long";

        private JwtAuthenticationFilter filter;

        @BeforeEach
        void setUp() {
                filter = new JwtAuthenticationFilter(SECRET);
                SecurityContextHolder.clearContext();
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void validTokenShouldAuthenticateUser() throws Exception {
                UUID userId = UUID.randomUUID();
                String token = createToken(userId.toString(), "RIDER");

                MockHttpServletRequest request = new MockHttpServletRequest();
                request.addHeader("Authorization", "Bearer " + token);

                filter.doFilter(
                                request,
                                new MockHttpServletResponse(),
                                new MockFilterChain());

                var authentication = SecurityContextHolder.getContext().getAuthentication();

                assertNotNull(authentication);
                assertTrue(authentication.getPrincipal() instanceof AuthenticatedUser);

                AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();

                assertEquals(userId, principal.getUserId());
                assertEquals("RIDER", principal.getRole());

                assertTrue(authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_RIDER")));
        }

        @Test
        void missingAuthorizationHeaderShouldRemainUnauthenticated()
                        throws Exception {
                MockHttpServletRequest request = new MockHttpServletRequest();

                filter.doFilter(
                                request,
                                new MockHttpServletResponse(),
                                new MockFilterChain());

                assertNull(
                                SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        void nonBearerHeaderShouldRemainUnauthenticated()
                        throws Exception {
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.addHeader("Authorization", "Basic abc123");

                filter.doFilter(
                                request,
                                new MockHttpServletResponse(),
                                new MockFilterChain());

                assertNull(
                                SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        void invalidTokenShouldRemainUnauthenticated() throws Exception {
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.addHeader("Authorization", "Bearer invalid-token");

                filter.doFilter(
                                request,
                                new MockHttpServletResponse(),
                                new MockFilterChain());

                assertNull(
                                SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        void invalidUserIdClaimShouldRemainUnauthenticated()
                        throws Exception {
                String token = createToken("not-a-uuid", "RIDER");

                MockHttpServletRequest request = new MockHttpServletRequest();
                request.addHeader("Authorization", "Bearer " + token);

                filter.doFilter(
                                request,
                                new MockHttpServletResponse(),
                                new MockFilterChain());

                assertNull(
                                SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        void tokenSignedWithDifferentSecretShouldRemainUnauthenticated()
                        throws Exception {
                String token = Jwts.builder()
                                .subject(UUID.randomUUID().toString())
                                .claim("role", "RIDER")
                                .signWith(Keys.hmacShaKeyFor(
                                                "another-test-secret-key-at-least-32-bytes".getBytes(
                                                                StandardCharsets.UTF_8)))
                                .compact();

                MockHttpServletRequest request = new MockHttpServletRequest();
                request.addHeader("Authorization", "Bearer " + token);

                filter.doFilter(
                                request,
                                new MockHttpServletResponse(),
                                new MockFilterChain());

                assertNull(
                                SecurityContextHolder.getContext().getAuthentication());
        }

        private String createToken(String subject, String role) {
                return Jwts.builder()
                                .subject(subject)
                                .claim("role", role)
                                .signWith(Keys.hmacShaKeyFor(
                                                SECRET.getBytes(StandardCharsets.UTF_8)))
                                .compact();
        }
}
