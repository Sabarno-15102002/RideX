package com.ridex.trip.config;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ridex.trip.dto.AuthenticatedUser;
import com.ridex.trip.exception.InvalidCredentialsException;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal()
                        instanceof AuthenticatedUser user)) {

            throw new InvalidCredentialsException(
                    "Authenticated user not found"
            );
        }

        return user.getUserId();
    }
}