package com.ridex.pricing.event;

import com.ridex.pricing.service.RiderIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

class RiderCreatedConsumerTest {

    private RiderIdentityService riderIdentityService;
    private RiderCreatedConsumer consumer;
    private RiderCreatedEvent event;

    @BeforeEach
    void setUp() {
        riderIdentityService = mock(RiderIdentityService.class);
        consumer = new RiderCreatedConsumer(riderIdentityService);

        event = new RiderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now());
    }

    @Test
    void shouldDelegateRiderCreatedEvent() {
        consumer.consume(event);

        verify(riderIdentityService).handleRiderCreated(event);
    }

    @Test
    void shouldPropagateServiceFailure() {
        doThrow(new RuntimeException("Database failure"))
                .when(riderIdentityService)
                .handleRiderCreated(event);

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> consumer.consume(event));
    }
}
