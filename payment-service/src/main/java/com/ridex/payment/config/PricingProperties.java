package com.ridex.payment.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ridex.pricing")
public record PricingProperties(
        String baseUrl,
        Timeout timeout,
        Retry retry
) {

    public record Timeout(
            Duration connect,
            Duration read
    ) {
    }

    public record Retry(
            int maxAttempts,
            Duration waitDuration,
            double exponentialBackoffMultiplier
    ) {
    }
}