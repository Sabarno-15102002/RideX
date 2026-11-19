package com.ridex.pricing.config;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PricingPropertiesTest {

    @Test
    void shouldUseDefaultPricingValues() {
        PricingProperties properties = new PricingProperties();

        assertEquals(new BigDecimal("50.00"), properties.getBaseFare());
        assertEquals(new BigDecimal("15.00"), properties.getPerKm());
        assertEquals(new BigDecimal("2.00"), properties.getPerMinute());
    }

    @Test
    void shouldAllowCustomPricingValues() {
        PricingProperties properties = new PricingProperties();

        properties.setBaseFare(new BigDecimal("75.00"));
        properties.setPerKm(new BigDecimal("18.50"));
        properties.setPerMinute(new BigDecimal("3.00"));

        assertEquals(new BigDecimal("75.00"), properties.getBaseFare());
        assertEquals(new BigDecimal("18.50"), properties.getPerKm());
        assertEquals(new BigDecimal("3.00"), properties.getPerMinute());
    }
}
