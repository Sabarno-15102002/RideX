package com.ridex.payment.config;

import com.ridex.payment.dto.AuthenticatedUser;
import com.ridex.payment.exception.InvalidCredentialsException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedUserId() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "RIDER");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null));

        assertEquals(userId, SecurityUtils.getCurrentUserId());
    }

    @Test
    void throwsWhenAuthenticationIsMissing() {
        assertThrows(InvalidCredentialsException.class,
                SecurityUtils::getCurrentUserId);
    }

    @Test
    void throwsWhenPrincipalHasWrongType() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("wrong-principal", null));

        assertThrows(InvalidCredentialsException.class,
                SecurityUtils::getCurrentUserId);
    }
}
