package com.ridex.trip.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.trip.event.event.DriverTripStartedEvent;
import com.ridex.trip.service.TripService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DriverTripStartedConsumer {

    private final TripService tripService;

    @KafkaListener(
            topics = "driver.trip.started",
            groupId = "trip-service",
            containerFactory = "driverTripStartedKafkaListenerContainerFactory"
    )
    public void consume(DriverTripStartedEvent event) {
        tripService.handleDriverTripStarted(event);
    }
}