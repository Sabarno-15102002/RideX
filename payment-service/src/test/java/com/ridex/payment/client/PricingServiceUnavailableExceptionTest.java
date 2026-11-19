package com.ridex.payment.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PricingServiceUnavailableExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        var exception = new PricingServiceUnavailableException(
                "Pricing Service is unavailable");

        assertEquals(
                "Pricing Service is unavailable",
                exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        var cause = new RuntimeException("Connection refused");

        var exception = new PricingServiceUnavailableException(
                "Pricing Service request failed",
                cause);

        assertEquals(
                "Pricing Service request failed",
                exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
