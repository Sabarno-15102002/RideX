package com.ridex.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "ridex.payment")
@Getter
@Setter
public class PaymentProperties {

    private String provider = "mock";
}