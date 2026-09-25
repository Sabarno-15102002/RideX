package com.ridex.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "payment_provider_events")
@Getter
@Setter
@NoArgsConstructor
public class PaymentProviderEvent {

    @Id
    @Column(name = "event_id", length = 255)
    private String eventId;

    @Column(name = "provider_payment_id", length = 255)
    private String providerPaymentId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public PaymentProviderEvent(
            String eventId,
            String providerPaymentId,
            String eventType,
            Instant receivedAt) {

        this.eventId = eventId;
        this.providerPaymentId = providerPaymentId;
        this.eventType = eventType;
        this.receivedAt = receivedAt;
    }
}