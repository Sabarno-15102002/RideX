package com.ridex.trip.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.trip.event.event.DriverRideRejectedEvent;
import com.ridex.trip.service.TripService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DriverRideRejectedConsumer {

    private final TripService tripService;

    @KafkaListener(
            topics = "driver.ride.rejected",
            groupId = "trip-service",
            containerFactory = "driverRideRejectedKafkaListenerContainerFactory"
    )
    public void consume(DriverRideRejectedEvent event) {
        tripService.handleDriverRideRejected(event);
    }
}