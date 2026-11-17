package com.ridex.driver.event;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import com.ridex.driver.service.DriverTripService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripCompletedConsumerTest {

        @Mock
        private DriverTripService driverTripService;

        @InjectMocks
        private TripCompletedConsumer consumer;

        @Test
        void consume_shouldDelegateEventToDriverTripService() {
                TripCompletedEvent event = new TripCompletedEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                12.9716,
                                77.5946,
                                12.9352,
                                77.6245,
                                Instant.now().minusSeconds(100),
                                Instant.now());

                consumer.consume(event);

                verify(driverTripService).handleTripCompleted(event);
        }

        @Test
        void consume_shouldPropagateServiceException() {
                TripCompletedEvent event = new TripCompletedEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                12.9716,
                                77.5946,
                                12.9352,
                                77.6245,
                                Instant.now().minusSeconds(100),
                                Instant.now());

                doThrow(new RuntimeException("Processing failed"))
                                .when(driverTripService)
                                .handleTripCompleted(event);

                org.junit.jupiter.api.Assertions.assertThrows(
                                RuntimeException.class,
                                () -> consumer.consume(event));
        }
}
