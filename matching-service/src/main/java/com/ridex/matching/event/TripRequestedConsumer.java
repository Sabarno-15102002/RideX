package com.ridex.matching.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.matching.service.MatchingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripRequestedConsumer {

    private final MatchingService matchingService;

    @KafkaListener(
            topics = "trip.requested",
            groupId = "matching-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TripRequestedEvent event) {

        log.info(
                "Received TRIP_REQUESTED event: eventId={}, riderId={}",
                event.eventId(),
                event.tripId(),
                event.riderId(),
                event.pickupLatitude(),
                event.pickupLongitude(),
                event.dropoffLatitude(),
                event.dropoffLongitude(),
                event.requestedAt()
        );

        matchingService.matchTrip(event);
    }
}
