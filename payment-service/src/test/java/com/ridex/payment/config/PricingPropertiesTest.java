package com.ridex.payment.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class PricingPropertiesTest {

    @Test
    void shouldCreatePropertiesWithAllValues() {
        var timeout = new PricingProperties.Timeout(
                Duration.ofSeconds(2),
                Duration.ofSeconds(5));

        var retry = new PricingProperties.Retry(
                3,
                Duration.ofMillis(200),
                2.0);

        var properties = new PricingProperties(
                "http://pricing-service",
                timeout,
                retry);

        assertEquals("http://pricing-service", properties.baseUrl());
        assertEquals(Duration.ofSeconds(2), properties.timeout().connect());
        assertEquals(Duration.ofSeconds(5), properties.timeout().read());
        assertEquals(3, properties.retry().maxAttempts());
        assertEquals(Duration.ofMillis(200), properties.retry().waitDuration());
        assertEquals(2.0, properties.retry().exponentialBackoffMultiplier());
    }

    @Test
    void shouldAllowNullNestedProperties() {
        var properties = new PricingProperties(
                "http://pricing-service",
                null,
                null);

        assertNull(properties.timeout());
        assertNull(properties.retry());
    }

    @Test
    void retryRecordShouldExposeConfiguredValues() {
        var retry = new PricingProperties.Retry(
                2,
                Duration.ofMillis(100),
                1.5);

        assertEquals(2, retry.maxAttempts());
        assertEquals(Duration.ofMillis(100), retry.waitDuration());
        assertEquals(1.5, retry.exponentialBackoffMultiplier());
    }
}
