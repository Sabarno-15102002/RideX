package com.ridex.payment.service.util;

import com.ridex.payment.event.EventSerializer;
import com.ridex.payment.event.PaymentCreatedEvent;
import com.ridex.payment.event.PaymentFailedEvent;
import com.ridex.payment.event.PaymentRefundedEvent;
import com.ridex.payment.event.PaymentSucceededEvent;
import com.ridex.payment.repository.OutboxEventRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    OutboxEventRepository repository;
    @Mock
    EventSerializer serializer;

    @InjectMocks
    OutboxEventService service;

    private final UUID paymentId = UUID.randomUUID();

    @Test
    void savesPaymentCreatedEvent() {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                UUID.randomUUID(), paymentId, UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("10.00"), "INR", Instant.now());
        when(serializer.serialize(event)).thenReturn("{\"type\":\"created\"}");

        service.savePaymentCreatedEvent(event);

        verify(repository).save(argThat(outbox -> outbox.getAggregateType().equals("PAYMENT")
                && outbox.getAggregateId().equals(paymentId)
                && outbox.getEventType().equals("PAYMENT_CREATED")
                && outbox.getPayload().equals("{\"type\":\"created\"}")));
    }

    @Test
    void savesPaymentSucceededEvent() {
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                UUID.randomUUID(), paymentId, UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("10.00"), "INR", Instant.now());
        when(serializer.serialize(event)).thenReturn("{}");

        service.savePaymentSucceededEvent(event);

        verify(repository).save(argThat(outbox -> outbox.getEventType().equals("PAYMENT_SUCCEEDED")
                && outbox.getAggregateId().equals(paymentId)));
    }

    @Test
    void savesPaymentFailedEvent() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(), paymentId, UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("10.00"), "INR", Instant.now());
        when(serializer.serialize(event)).thenReturn("{}");

        service.savePaymentFailedEvent(event);

        verify(repository).save(argThat(outbox -> outbox.getEventType().equals("PAYMENT_FAILED")));
    }

    @Test
    void savesPaymentRefundedEvent() {
        PaymentRefundedEvent event = new PaymentRefundedEvent(
                UUID.randomUUID(), paymentId, UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("10.00"), "INR", Instant.now());
        when(serializer.serialize(event)).thenReturn("{}");

        service.savePaymentRefundedEvent(event);

        verify(repository).save(argThat(outbox -> outbox.getEventType().equals("PAYMENT_REFUNDED")));
    }
}
