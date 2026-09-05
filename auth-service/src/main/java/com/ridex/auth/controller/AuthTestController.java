package com.ridex.auth.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class AuthTestController {

    @GetMapping("/me")
    public Map<String, Object> me(
            Authentication authentication) {

        return Map.of(
                "userId", authentication.getPrincipal(),
                "authorities", authentication.getAuthorities()
        );
    }
}