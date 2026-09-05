package com.ridex.rider.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.rider.service.RiderRegistrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredConsumer {

    private final RiderRegistrationService riderRegistrationService;

    @KafkaListener(
            topics = "user.registered",
            groupId = "rider-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(UserRegisteredEvent event) {

        log.info(
                "Received USER_REGISTERED event: eventId={}, userId={}",
                event.eventId(),
                event.userId()
        );

        riderRegistrationService.handleUserRegistered(event);
    }
}