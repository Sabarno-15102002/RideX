package com.ridex.payment.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentPropertiesTest {

    @Test
    void defaultsToMockProvider() {
        PaymentProperties properties = new PaymentProperties();

        assertEquals("mock", properties.getProvider());
    }

    @Test
    void allowsProviderToBeConfigured() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("custom");

        assertEquals("custom", properties.getProvider());
    }
}
