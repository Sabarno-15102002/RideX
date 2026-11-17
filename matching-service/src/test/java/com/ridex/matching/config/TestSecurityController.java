package com.ridex.matching.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestSecurityController {

    @GetMapping("/actuator/test")
    public String actuator() {
        return "actuator-accessible";
    }

    @GetMapping("/protected")
    public String protectedEndpoint() {
        return "authenticated";
    }
}