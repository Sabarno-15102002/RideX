package com.ridex.trip.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ridex.trip.dto.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "trip_payments")
public class TripPayment {

    @Id
    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TripPayment() {
    }

    public TripPayment(
            UUID tripId,
            UUID paymentId,
            UUID riderId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            Instant createdAt
    ) {
        this.tripId = tripId;
        this.paymentId = paymentId;
        this.riderId = riderId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getTripId() {
        return tripId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getRiderId() {
        return riderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markSuccess(Instant paidAt) {
        this.status = PaymentStatus.SUCCESS;
        this.paidAt = paidAt;
        this.updatedAt = paidAt;
    }

    public void markFailed(Instant updatedAt) {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = updatedAt;
    }

    public void markRefunded(Instant refundedAt) {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = refundedAt;
        this.updatedAt = refundedAt;
    }
}