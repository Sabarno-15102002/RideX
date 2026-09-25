package com.ridex.payment.service.provider;

import org.springframework.stereotype.Component;

import com.ridex.payment.config.PaymentProperties;
import com.ridex.payment.service.PaymentProvider;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentProviderFactory {

    private final PaymentProperties properties;
    private final MockPaymentProvider mockPaymentProvider;

    public PaymentProvider getProvider() {

        return switch (properties.getProvider().toLowerCase()) {
            case "mock" -> mockPaymentProvider;

            default -> throw new IllegalStateException(
                    "Unsupported payment provider: "
                            + properties.getProvider()
            );
        };
    }
}