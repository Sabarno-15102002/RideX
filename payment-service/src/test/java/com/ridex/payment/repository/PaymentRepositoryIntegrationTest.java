package com.ridex.payment.repository;

import com.ridex.payment.dto.PaymentStatus;
import com.ridex.payment.entity.Payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PaymentRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    PaymentRepository repository;

    private Payment payment(UUID tripId, UUID riderId) {
        Payment payment = new Payment();
        payment.setTripId(tripId);
        payment.setRiderId(riderId);
        payment.setAmount(new BigDecimal("120.50"));
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey("trip:" + tripId);
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        return payment;
    }

    @Test
    void savesAndRetrievesPaymentById() {
        Payment saved = repository.saveAndFlush(
                payment(UUID.randomUUID(), UUID.randomUUID()));

        Payment found = repository.findById(saved.getId()).orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertEquals(new BigDecimal("120.50"), found.getAmount());
        assertEquals(PaymentStatus.PENDING, found.getStatus());
    }

    @Test
    void findsPaymentByTripId() {
        UUID tripId = UUID.randomUUID();
        Payment saved = repository.saveAndFlush(
                payment(tripId, UUID.randomUUID()));

        assertEquals(saved.getId(),
                repository.findByTripId(tripId).orElseThrow().getId());
    }

    @Test
    void findsPaymentByRiderAndTrip() {
        UUID riderId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        Payment saved = repository.saveAndFlush(payment(tripId, riderId));

        assertEquals(saved.getId(),
                repository.findByRiderIdAndTripId(riderId, tripId)
                        .orElseThrow().getId());
    }

    @Test
    void doesNotFindPaymentForAnotherRider() {
        UUID tripId = UUID.randomUUID();
        repository.saveAndFlush(payment(tripId, UUID.randomUUID()));

        assertTrue(repository.findByRiderIdAndTripId(
                UUID.randomUUID(), tripId).isEmpty());
    }

    @Test
    void checksWhetherTripAlreadyHasPayment() {
        UUID tripId = UUID.randomUUID();

        assertFalse(repository.existsByTripId(tripId));

        repository.saveAndFlush(payment(tripId, UUID.randomUUID()));

        assertTrue(repository.existsByTripId(tripId));
    }

    @Test
    void findsPaymentByProviderPaymentId() {
        Payment payment = payment(UUID.randomUUID(), UUID.randomUUID());
        payment.setProviderPaymentId("provider-payment-1");
        Payment saved = repository.saveAndFlush(payment);

        assertEquals(saved.getId(),
                repository.findByProviderPaymentId("provider-payment-1")
                        .orElseThrow().getId());
    }

    @Test
    void locksPaymentById() {
        Payment saved = repository.saveAndFlush(
                payment(UUID.randomUUID(), UUID.randomUUID()));

        assertTrue(repository.findByIdForUpdate(saved.getId()).isPresent());
    }
}
