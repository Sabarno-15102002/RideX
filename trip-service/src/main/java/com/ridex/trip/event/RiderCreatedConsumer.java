package com.ridex.trip.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.trip.service.RiderIdentityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiderCreatedConsumer {

    private final RiderIdentityService riderIdentityService;

    @KafkaListener(
            topics = "rider.created",
            groupId = "trip-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(RiderCreatedEvent event) {

        log.info(
                "Received RIDER_CREATED: eventId={}, riderId={}, userId={}",
                event.eventId(),
                event.riderId(),
                event.userId()
        );

        riderIdentityService.handleRiderCreated(event);
    }
}