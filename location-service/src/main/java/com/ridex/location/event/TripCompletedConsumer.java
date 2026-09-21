package com.ridex.location.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.location.service.DriverLocationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TripCompletedConsumer {

    private final DriverLocationService driverLocationService;
    @KafkaListener(
            topics = "trip.completed",
            groupId = "location-service",
            containerFactory = "tripCompletedKafkaListenerContainerFactory"
    )
    public void consume(TripCompletedEvent event) {

        driverLocationService.handleTripCompleted(event);
    }
}