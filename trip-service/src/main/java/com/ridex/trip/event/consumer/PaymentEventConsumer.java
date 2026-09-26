package com.ridex.trip.event.consumer;

import java.time.Instant;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.trip.entity.ProcessedEvent;
import com.ridex.trip.event.event.PaymentFailedEvent;
import com.ridex.trip.event.event.PaymentRefundedEvent;
import com.ridex.trip.event.event.PaymentSucceededEvent;
import com.ridex.trip.repository.ProcessedEventRepository;
import com.ridex.trip.service.TripPaymentProjection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class PaymentEventConsumer {

    private final TripPaymentProjection projection;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "payment.succeeded",
            groupId = "trip-service",
            containerFactory = "paymentSuccededKafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentSucceeded(PaymentSucceededEvent event) {

        if(processedEventRepository.existsById(event.eventId())){
            return;
        }
        projection.project(event);

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }

    @KafkaListener(
            topics = "payment.failed",
            groupId = "trip-service",
            containerFactory = "paymentFailedKafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {

        if(processedEventRepository.existsById(event.eventId())){
            return;
        }
        projection.project(event);

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }

    @KafkaListener(
            topics = "payment.refunded",
            groupId = "trip-service",
            containerFactory = "paymentRefundedKafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentRefunded(PaymentRefundedEvent event) {

        if(processedEventRepository.existsById(event.eventId())){
            return;
        }
        projection.project(event);

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }
}