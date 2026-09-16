package com.ridex.trip.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.trip.event.event.DriverMatchRequestedEvent;
import com.ridex.trip.service.TripService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DriverMatchRequestedConsumer {

    private final TripService tripService;

    @KafkaListener(
            topics = "driver.match.requested",
            groupId = "trip-service",
            containerFactory = "driverMatchRequestedKafkaListenerContainerFactory"
    )
    public void consume(DriverMatchRequestedEvent event) {

        log.info(
                "Received DRIVER_MATCH_REQUESTED: eventId={}, tripId={}, driverId={}",
                event.eventId(),
                event.tripId(),
                event.driverId()
        );

        tripService.assignDriver(event);
    }
}