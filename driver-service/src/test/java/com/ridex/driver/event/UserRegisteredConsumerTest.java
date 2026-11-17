package com.ridex.driver.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import com.ridex.driver.service.DriverRegistrationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRegisteredConsumerTest {

        @Mock
        private DriverRegistrationService registrationService;

        @InjectMocks
        private UserRegisteredConsumer consumer;

        @Test
        void consume_shouldDelegateEventToRegistrationService() {
                UserRegisteredEvent event = new UserRegisteredEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "John",
                                "DRIVER",
                                Instant.now());

                consumer.consume(event);

                verify(registrationService).handleUserRegistered(event);
        }

        @Test
        void consume_shouldPropagateServiceException() {
                UserRegisteredEvent event = new UserRegisteredEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "John",
                                "DRIVER",
                                Instant.now());

                doThrow(new RuntimeException("Registration failed"))
                                .when(registrationService)
                                .handleUserRegistered(event);

                assertThrows(
                                RuntimeException.class,
                                () -> consumer.consume(event));
        }
}
