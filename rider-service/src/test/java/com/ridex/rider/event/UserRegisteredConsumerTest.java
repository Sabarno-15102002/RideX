package com.ridex.rider.event;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import com.ridex.rider.service.RiderRegistrationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRegisteredConsumerTest {

        @Mock
        private RiderRegistrationService riderRegistrationService;

        @InjectMocks
        private UserRegisteredConsumer consumer;

        @Test
        void consume_shouldDelegateEventToRegistrationService() {
                UserRegisteredEvent event = new UserRegisteredEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "John Rider",
                                "RIDER",
                                Instant.now());

                consumer.consume(event);

                verify(riderRegistrationService).handleUserRegistered(event);
        }

        @Test
        void consume_shouldPropagateRegistrationServiceException() {
                UserRegisteredEvent event = new UserRegisteredEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "John Rider",
                                "RIDER",
                                Instant.now());

                doThrow(new RuntimeException("Registration failed"))
                                .when(riderRegistrationService)
                                .handleUserRegistered(event);

                org.junit.jupiter.api.Assertions.assertThrows(
                                RuntimeException.class,
                                () -> consumer.consume(event));

                verify(riderRegistrationService).handleUserRegistered(event);
        }
}
