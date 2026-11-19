package com.ridex.payment.event;

import com.ridex.payment.repository.ProcessedEventRepository;
import com.ridex.payment.service.RiderIdentityService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiderCreatedEventListenerTest {

    @Mock
    RiderIdentityService riderIdentityService;
    @Mock
    ProcessedEventRepository processedEventRepository;

    @InjectMocks
    RiderCreatedEventListener listener;

    @Test
    void handlesNewRiderCreatedEvent() {
        RiderCreatedEvent event = new RiderCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), Instant.now());

        when(processedEventRepository.existsById(event.eventId())).thenReturn(false);

        listener.handle(event);

        verify(riderIdentityService).createIdentity(event);
        verify(processedEventRepository).save(argThat(processed -> processed.getEventId().equals(event.eventId())));
    }

    @Test
    void ignoresAlreadyProcessedRiderEvent() {
        RiderCreatedEvent event = new RiderCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), Instant.now());

        when(processedEventRepository.existsById(event.eventId())).thenReturn(true);

        listener.handle(event);

        verifyNoInteractions(riderIdentityService);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void doesNotMarkEventProcessedWhenIdentityCreationFails() {
        RiderCreatedEvent event = new RiderCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), Instant.now());

        when(processedEventRepository.existsById(event.eventId())).thenReturn(false);
        doThrow(new RuntimeException("database unavailable"))
                .when(riderIdentityService).createIdentity(event);

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class, () -> listener.handle(event));

        verify(processedEventRepository, never()).save(any());
    }
}
