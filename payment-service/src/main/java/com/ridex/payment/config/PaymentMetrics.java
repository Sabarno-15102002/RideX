package com.ridex.payment.config;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class PaymentMetrics {
    
    private final Counter paymentsSucceeded;
    private final Counter paymentsFailed;
    private final Counter paymentsRefunded;
    private final Timer paymentProcessingDuration;

    public PaymentMetrics(MeterRegistry meterRegistry) {
        this.paymentsSucceeded = Counter.builder("ridex.payments.succeeded")
                .description("Number of successful payments")
                .register(meterRegistry);

        this.paymentsFailed = Counter.builder("ridex.payments.failed")
                .description("Number of failed payments")
                .register(meterRegistry);

        this.paymentsRefunded = Counter.builder("ridex.payments.refunded")
                .description("Number of refunded payments")
                .register(meterRegistry);

        this.paymentProcessingDuration = Timer.builder("ridex.payments.processing.duration")
                .description("Duration of payment processing")
                .publishPercentiles(0.50, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void recordPaymentSucceeded() {
        paymentsSucceeded.increment();
    }

    public void recordPaymentFailed() {
        paymentsFailed.increment();
    }

    public void recordPaymentRefunded() {
        paymentsRefunded.increment();
    }

    public void recordPaymentProcessingDuration(long duration) {
        paymentProcessingDuration.record(duration, TimeUnit.MILLISECONDS);
    }
}
