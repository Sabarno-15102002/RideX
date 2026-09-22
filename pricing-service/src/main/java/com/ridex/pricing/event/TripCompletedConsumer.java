package com.ridex.pricing.event;

import java.time.Instant;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.pricing.entity.ProcessedEvent;
import com.ridex.pricing.repository.ProcessedEventRepository;
import com.ridex.pricing.service.FareCalculationService;


import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TripCompletedConsumer {

    private final FareCalculationService fareCalculationService;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "trip.completed",
            groupId = "pricing-service",
            containerFactory = "tripCompletedKafkaListenerContainerFactory"
    )
    @Transactional 
    public void consume(TripCompletedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        fareCalculationService.createFareQuote(event);

        processedEventRepository.save(
            new ProcessedEvent(
                event.eventId(),
                Instant.now()
            )
        );
    }
}