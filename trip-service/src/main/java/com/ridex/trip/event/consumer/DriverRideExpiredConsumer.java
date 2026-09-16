package com.ridex.trip.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.trip.event.event.DriverRideExpiredEvent;
import com.ridex.trip.service.TripService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DriverRideExpiredConsumer {

    private final TripService tripService;

    @KafkaListener(
            topics = "driver.ride.expired",
            groupId = "trip-service",
            containerFactory = "driverRideExpiredKafkaListenerContainerFactory"
    )
    public void consume(DriverRideExpiredEvent event) {
        tripService.handleDriverRideExpired(event);
    }
}