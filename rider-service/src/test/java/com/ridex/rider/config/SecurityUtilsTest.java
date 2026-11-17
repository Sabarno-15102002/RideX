package com.ridex.rider.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ridex.rider.dto.AuthenticatedUser;
import com.ridex.rider.exception.InvalidCredentialsException;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_shouldReturnAuthenticatedUserId() {
        UUID userId = UUID.randomUUID();

        AuthenticatedUser principal =
                new AuthenticatedUser(userId, "RIDER");

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        java.util.List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        assertEquals(userId, SecurityUtils.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_shouldThrowWhenAuthenticationIsMissing() {
        SecurityContextHolder.clearContext();

        assertThrows(
                InvalidCredentialsException.class,
                SecurityUtils::getCurrentUserId
        );
    }

    @Test
    void getCurrentUserId_shouldThrowWhenPrincipalIsNotAuthenticatedUser() {
        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "some-user",
                        null,
                        java.util.List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        assertThrows(
                InvalidCredentialsException.class,
                SecurityUtils::getCurrentUserId
        );
    }
}
