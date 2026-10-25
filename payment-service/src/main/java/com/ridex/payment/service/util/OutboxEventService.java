package com.ridex.payment.service.util;

import org.springframework.stereotype.Service;

import com.ridex.payment.entity.OutboxEvent;
import com.ridex.payment.event.EventSerializer;
import com.ridex.payment.event.PaymentCreatedEvent;
import com.ridex.payment.event.PaymentFailedEvent;
import com.ridex.payment.event.PaymentRefundedEvent;
import com.ridex.payment.event.PaymentSucceededEvent;
import com.ridex.payment.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final EventSerializer eventSerializer;

    public void savePaymentSucceededEvent(PaymentSucceededEvent event) {

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(event.paymentId())
                .eventType("PAYMENT_SUCCEEDED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }

    public void savePaymentFailedEvent(PaymentFailedEvent event) {

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(event.paymentId())
                .eventType("PAYMENT_FAILED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }    

    public void savePaymentRefundedEvent(PaymentRefundedEvent event) {

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(event.paymentId())
                .eventType("PAYMENT_REFUNDED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }
    
    public void savePaymentCreatedEvent(PaymentCreatedEvent event) {

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(event.paymentId())
                .eventType("PAYMENT_CREATED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }

}