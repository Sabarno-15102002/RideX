package com.ridex.payment.service.Impl;

import com.ridex.payment.config.PaymentMetrics;
import com.ridex.payment.dto.PaymentAttemptStatus;
import com.ridex.payment.dto.PaymentProviderWebhook;
import com.ridex.payment.dto.PaymentStatus;
import com.ridex.payment.entity.Payment;
import com.ridex.payment.entity.PaymentAttempt;
import com.ridex.payment.repository.PaymentAttemptRepository;
import com.ridex.payment.repository.PaymentProviderEventRepository;
import com.ridex.payment.repository.PaymentRepository;
import com.ridex.payment.service.util.OutboxEventService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({
        PaymentWebhookServiceImpl.class,
        PaymentWebhookPostgresIntegrationTest.TestConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentWebhookPostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        OutboxEventService outboxEventService() {
            return mock(OutboxEventService.class);
        }

        @Bean
        PaymentMetrics paymentMetrics() {
            return new PaymentMetrics(new SimpleMeterRegistry());
        }
    }

    @Autowired
    PaymentWebhookServiceImpl service;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    PaymentAttemptRepository attemptRepository;
    @Autowired
    PaymentProviderEventRepository providerEventRepository;

    @Test
    void successfulWebhookUpdatesAllPaymentRecords() {
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

        Payment payment = new Payment();
        payment.setTripId(tripId);
        payment.setRiderId(riderId);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey("trip:" + tripId);
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        payment = paymentRepository.saveAndFlush(payment);

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setPaymentId(payment.getId());
        attempt.setAttemptNumber(1);
        attempt.setIdempotencyKey("payment:" + payment.getId() + ":attempt:1");
        attempt.setProviderPaymentId("provider-postgres-1");
        attempt.setStatus(PaymentAttemptStatus.PROCESSING);
        attempt.setCreatedAt(Instant.now());
        attempt.setUpdatedAt(Instant.now());
        attemptRepository.saveAndFlush(attempt);

        service.process(new PaymentProviderWebhook(
                "provider-postgres-1", "idem-1", "SUCCESS",
                "evt-postgres-1", null));

        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        PaymentAttempt updatedAttempt = attemptRepository.findById(attempt.getId()).orElseThrow();

        assertEquals(PaymentStatus.SUCCESS, updatedPayment.getStatus());
        assertEquals(PaymentAttemptStatus.SUCCESS, updatedAttempt.getStatus());
        assertTrue(providerEventRepository.existsById("evt-postgres-1"));
    }

    @Test
    void duplicateWebhookDoesNotApplyAgain() {
        UUID tripId = UUID.randomUUID();

        Payment payment = new Payment();
        payment.setTripId(tripId);
        payment.setRiderId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey("trip:" + tripId);
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        payment = paymentRepository.saveAndFlush(payment);

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setPaymentId(payment.getId());
        attempt.setAttemptNumber(1);
        attempt.setIdempotencyKey("payment:" + payment.getId() + ":attempt:1");
        attempt.setProviderPaymentId("provider-postgres-2");
        attempt.setStatus(PaymentAttemptStatus.PROCESSING);
        attempt.setCreatedAt(Instant.now());
        attempt.setUpdatedAt(Instant.now());
        attemptRepository.saveAndFlush(attempt);

        PaymentProviderWebhook webhook = new PaymentProviderWebhook(
                "provider-postgres-2", "idem-2", "SUCCESS",
                "evt-postgres-2", null);

        service.process(webhook);
        service.process(webhook);

        assertEquals(PaymentStatus.SUCCESS,
                paymentRepository.findById(payment.getId()).orElseThrow().getStatus());
        assertTrue(providerEventRepository.existsById("evt-postgres-2"));
    }
}
