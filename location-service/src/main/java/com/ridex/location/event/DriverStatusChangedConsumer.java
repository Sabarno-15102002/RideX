package com.ridex.location.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.location.service.DriverLocationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DriverStatusChangedConsumer {

    private final DriverLocationService driverLocationService;

    @KafkaListener(
            topics = "driver.status.changed",
            groupId = "location-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(DriverStatusChangedEvent event) {

        log.info(
                "Received DRIVER_STATUS_CHANGED: eventId={}, driverId={}, status={}",
                event.eventId(),
                event.driverId(),
                event.status()
        );

        driverLocationService.updateDriverIdentity(event.userId(), event.driverId());
        driverLocationService.updateDriverStatus(event.driverId(),event.status());
        driverLocationService.updateDriverAvailability(event);
    }
}