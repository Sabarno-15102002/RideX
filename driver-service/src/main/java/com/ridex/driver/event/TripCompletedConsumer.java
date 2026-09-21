package com.ridex.driver.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.driver.service.DriverTripService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TripCompletedConsumer {

    private final DriverTripService driverTripService;

    @KafkaListener(
            topics = "trip.completed",
            groupId = "driver-service",
            containerFactory = "tripCompletedKafkaListenerContainerFactory"
    )
    public void consume(TripCompletedEvent event) {
        driverTripService.handleTripCompleted(event);
    }
}