package com.ridex.trip.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.trip.event.event.DriverRideAcceptedEvent;
import com.ridex.trip.service.TripService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DriverRideAcceptedConsumer {
    private final TripService tripService;

    @KafkaListener(
            topics = "driver.ride.accepted",
            groupId = "trip-service",
            containerFactory = "driverRideAcceptedKafkaListenerContainerFactory"
    )
    public void consume(DriverRideAcceptedEvent event) {

        log.info(
                "Received DRIVER_RIDE_ACCEPTED: eventId={}, tripId={}, driverId={}",
                event.eventId(),
                event.tripId(),
                event.driverId()
        );

        tripService.handleDriverRideAccepted(event);
    }
}
