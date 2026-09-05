package com.ridex.auth.dto.request;

import com.ridex.auth.utilities.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(

        @NotBlank
        String name,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password,

        String phone,

        @NotNull
        UserRole role
) {
}