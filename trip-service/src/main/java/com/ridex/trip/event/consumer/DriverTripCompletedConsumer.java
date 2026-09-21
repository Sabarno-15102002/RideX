package com.ridex.trip.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.trip.event.event.DriverTripCompletedEvent;
import com.ridex.trip.service.TripService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DriverTripCompletedConsumer {

    private final TripService tripService;

    @KafkaListener(
            topics = "driver.trip.completed",
            groupId = "trip-service",
            containerFactory = "driverTripCompletedKafkaListenerContainerFactory"
    )
    public void consume(DriverTripCompletedEvent event) {
        tripService.handleDriverTripCompleted(event);
    }
}