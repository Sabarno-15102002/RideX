package com.ridex.pricing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "ridex.pricing")
public class PricingProperties {

    private BigDecimal baseFare = new BigDecimal("50.00");
    private BigDecimal perKm = new BigDecimal("15.00");
    private BigDecimal perMinute = new BigDecimal("2.00");
}