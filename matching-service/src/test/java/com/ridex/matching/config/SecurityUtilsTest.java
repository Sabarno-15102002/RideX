package com.ridex.matching.config;

import com.ridex.matching.dto.AuthenticatedUser;
import com.ridex.matching.exception.InvalidCredentialsException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_shouldReturnAuthenticatedUserId() {
        UUID userId = UUID.randomUUID();

        AuthenticatedUser user = new AuthenticatedUser(userId, "RIDER");

        var authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                java.util.List.of());

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        UUID result = SecurityUtils.getCurrentUserId();

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void getCurrentUserId_shouldThrowWhenAuthenticationIsMissing() {
        SecurityContextHolder.clearContext();

        assertThrows(
                InvalidCredentialsException.class,
                SecurityUtils::getCurrentUserId);
    }

    @Test
    void getCurrentUserId_shouldThrowWhenPrincipalIsNotAuthenticatedUser() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "some-user",
                null,
                java.util.List.of());

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        assertThrows(
                InvalidCredentialsException.class,
                SecurityUtils::getCurrentUserId);
    }
}
