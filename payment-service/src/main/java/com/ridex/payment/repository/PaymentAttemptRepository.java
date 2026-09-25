package com.ridex.payment.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ridex.payment.entity.PaymentAttempt;

import jakarta.persistence.LockModeType;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    Optional<PaymentAttempt> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentAttempt> findByProviderPaymentId(String providerPaymentId);

    Optional<PaymentAttempt> findTopByPaymentIdOrderByAttemptNumberDesc(UUID paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM PaymentAttempt a
        WHERE a.providerPaymentId = :providerPaymentId
    """)
    Optional<PaymentAttempt> findByProviderPaymentIdForUpdate(
            @Param("providerPaymentId") String providerPaymentId
    );
}
