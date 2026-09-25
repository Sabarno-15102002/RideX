package com.ridex.payment.event;

import java.time.Instant;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.payment.client.PricingServiceClient;
import com.ridex.payment.dto.response.FareQuoteResponse;
import com.ridex.payment.entity.ProcessedEvent;
import com.ridex.payment.repository.ProcessedEventRepository;
import com.ridex.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TripCompletedConsumer {

    private final PaymentService paymentService;
    private final PricingServiceClient pricingServiceClient;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "trip.completed", groupId = "payment-service")
    @Transactional
    public void handle(TripCompletedEvent event) {

        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        FareQuoteResponse fare = pricingServiceClient.getFareQuote(event.tripId());

        paymentService.createPendingPayment(event, fare);

        processedEventRepository.save(
                new ProcessedEvent(
                        event.eventId(),
                        Instant.now()));
    }
}