package com.ridex.driver.config;

import com.ridex.driver.dto.AuthenticatedUser;
import com.ridex.driver.exception.InvalidCredentialsException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_withAuthenticatedUser_shouldReturnUserId() {
        UUID userId = UUID.randomUUID();

        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "DRIVER"),
                null
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        assertEquals(userId, SecurityUtils.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_withoutAuthentication_shouldThrowException() {
        SecurityContextHolder.clearContext();

        assertThrows(
                InvalidCredentialsException.class,
                SecurityUtils::getCurrentUserId
        );
    }

    @Test
    void getCurrentUserId_withWrongPrincipalType_shouldThrowException() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "some-user",
                null
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        assertThrows(
                InvalidCredentialsException.class,
                SecurityUtils::getCurrentUserId
        );
    }
}
