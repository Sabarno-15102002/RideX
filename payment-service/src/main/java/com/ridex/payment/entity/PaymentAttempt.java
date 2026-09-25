package com.ridex.payment.entity;

import java.time.Instant;
import java.util.UUID;

import com.ridex.payment.dto.PaymentAttemptStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "payment_attempts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_attempts_payment_number",
                        columnNames = {"payment_id", "attempt_number"}
                ),
                @UniqueConstraint(
                        name = "uk_payment_attempts_idempotency_key",
                        columnNames = "idempotency_key"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PaymentAttempt {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 255
    )
    private String idempotencyKey;

    @Column(name = "provider_payment_id", length = 255)
    private String providerPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private PaymentAttemptStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PaymentAttempt(
            UUID id,
            UUID paymentId,
            Integer attemptNumber,
            String idempotencyKey,
            PaymentAttemptStatus status,
            Instant createdAt,
            Instant updatedAt) {

        this.id = id;
        this.paymentId = paymentId;
        this.attemptNumber = attemptNumber;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}