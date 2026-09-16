package com.ridex.matching.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.matching.service.MatchingService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TripRematchingConsumer {

    private final MatchingService matchingService;

    @KafkaListener(
            topics = "trip.rematching",
            groupId = "matching-service",
            containerFactory = "tripRematchingKafkaListenerContainerFactory"
    )
    public void consume(TripRematchingEvent event) {

        matchingService.rematch(event);
    }
}