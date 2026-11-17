package com.ridex.matching.event;

import com.ridex.matching.service.MatchingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripRequestedConsumerTest {

    @Mock
    private MatchingService matchingService;

    @InjectMocks
    private TripRequestedConsumer consumer;

    private TripRequestedEvent event;

    @BeforeEach
    void setUp() {
        event = new TripRequestedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                12.9716,
                77.5946,
                12.9352,
                77.6245,
                Instant.now()
        );
    }

    @Test
    void consume_shouldDelegateEventToMatchingService() {
        consumer.consume(event);

        verify(matchingService).matchTrip(event);
    }

    @Test
    void consume_shouldPropagateMatchingServiceException() {
        doThrow(new IllegalStateException("Matching failed"))
                .when(matchingService).matchTrip(event);

        assertThrows(
                IllegalStateException.class,
                () -> consumer.consume(event)
        );

        verify(matchingService).matchTrip(event);
    }
}
