package com.ridex.payment.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMetricsTest {

    @Test
    void registersPaymentMetricsAndRecordsMeasurements() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PaymentMetrics metrics = new PaymentMetrics(registry);

        metrics.recordPaymentSucceeded();
        metrics.recordPaymentFailed();
        metrics.recordPaymentRefunded();
        metrics.recordPaymentProcessingDuration(250);

        assertEquals(1.0, registry.get("ridex.payments.succeeded").counter().count());
        assertEquals(1.0, registry.get("ridex.payments.failed").counter().count());
        assertEquals(1.0, registry.get("ridex.payments.refunded").counter().count());
        assertEquals(1L, registry.get("ridex.payments.processing.duration").timer().count());
    }
}
