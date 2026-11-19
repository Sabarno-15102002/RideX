
package com.ridex.pricing.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestSecurityController {

    @GetMapping("/test/protected")
    String protectedEndpoint() {
        return "authenticated";
    }

    @GetMapping("/api/v1/internal/test")
    String internalEndpoint() {
        return "internal";
    }

    @GetMapping("/actuator/test")
    String actuatorEndpoint() {
        return "actuator";
    }
}
