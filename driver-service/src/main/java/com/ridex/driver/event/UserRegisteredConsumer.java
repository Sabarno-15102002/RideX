package com.ridex.driver.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ridex.driver.service.DriverRegistrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredConsumer {

    private final DriverRegistrationService driverRegistrationService;

    @KafkaListener(
            topics = "user.registered",
            groupId = "driver-service",
            containerFactory = "driverRegisteredKafkaListenerContainerFactory"
    )
    public void consume(UserRegisteredEvent event) {

        log.info(
                "Received USER_REGISTERED event: eventId={}, userId={}, role={}",
                event.eventId(),
                event.userId(),
                event.role()
        );

        driverRegistrationService.handleUserRegistered(event);
    }
}