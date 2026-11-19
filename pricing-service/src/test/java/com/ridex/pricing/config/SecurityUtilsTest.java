package com.ridex.pricing.config;

import com.ridex.pricing.dto.AuthenticatedUser;
import com.ridex.pricing.exception.InvalidCredentialsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUserId() {
        UUID userId = UUID.randomUUID();

        AuthenticatedUser user = new AuthenticatedUser(userId, "RIDER");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        java.util.List.of()));

        assertEquals(userId, SecurityUtils.getCurrentUserId());
    }

    @Test
    void shouldThrowWhenAuthenticationIsMissing() {
        SecurityContextHolder.clearContext();

        assertThrows(
                InvalidCredentialsException.class,
                SecurityUtils::getCurrentUserId);
    }

    @Test
    void shouldThrowWhenPrincipalIsNotAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "some-user",
                        null,
                        java.util.List.of()));

        assertThrows(
                InvalidCredentialsException.class,
                SecurityUtils::getCurrentUserId);
    }
}
