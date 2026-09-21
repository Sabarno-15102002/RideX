package com.ridex.trip.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.trip.event.event.DriverArrivedEvent;
import com.ridex.trip.service.TripService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DriverArrivedConsumer {

    private final TripService tripService;

    @KafkaListener(
            topics = "driver.arrived",
            groupId = "trip-service",
            containerFactory = "driverArrivedKafkaListenerContainerFactory"
    )
    public void consume(DriverArrivedEvent event) {
        tripService.handleDriverArrived(event);
    }
}