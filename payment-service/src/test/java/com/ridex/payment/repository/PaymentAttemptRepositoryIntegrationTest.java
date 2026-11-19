package com.ridex.payment.repository;

import com.ridex.payment.dto.PaymentAttemptStatus;
import com.ridex.payment.entity.PaymentAttempt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PaymentAttemptRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    PaymentAttemptRepository repository;

    private PaymentAttempt attempt(UUID paymentId, int number, String key) {
        Instant now = Instant.now();
        return new PaymentAttempt(
                UUID.randomUUID(), paymentId, number, key,
                PaymentAttemptStatus.CREATED, now, now);
    }

    @Test
    void savesAndFindsAttemptByIdempotencyKey() {
        PaymentAttempt saved = repository.saveAndFlush(
                attempt(UUID.randomUUID(), 1, "idem-1"));

        assertEquals(saved.getId(),
                repository.findByIdempotencyKey("idem-1").orElseThrow().getId());
    }

    @Test
    void findsLatestAttemptForPayment() {
        UUID paymentId = UUID.randomUUID();

        repository.saveAndFlush(attempt(paymentId, 1, "idem-1"));
        PaymentAttempt latest = repository.saveAndFlush(
                attempt(paymentId, 2, "idem-2"));

        assertEquals(latest.getId(),
                repository.findTopByPaymentIdOrderByAttemptNumberDesc(paymentId)
                        .orElseThrow().getId());
    }

    @Test
    void findsAttemptByProviderPaymentId() {
        PaymentAttempt attempt = attempt(UUID.randomUUID(), 1, "idem-provider");
        attempt.setProviderPaymentId("provider-123");

        PaymentAttempt saved = repository.saveAndFlush(attempt);

        assertEquals(saved.getId(),
                repository.findByProviderPaymentId("provider-123")
                        .orElseThrow().getId());
    }

    @Test
    void findsAttemptForUpdateByProviderPaymentId() {
        PaymentAttempt attempt = attempt(UUID.randomUUID(), 1, "idem-lock");
        attempt.setProviderPaymentId("provider-lock");

        repository.saveAndFlush(attempt);

        assertTrue(repository.findByProviderPaymentIdForUpdate("provider-lock")
                .isPresent());
    }
}
