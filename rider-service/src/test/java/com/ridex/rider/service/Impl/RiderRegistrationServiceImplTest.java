package com.ridex.rider.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import com.ridex.rider.entity.ProcessedEvent;
import com.ridex.rider.entity.Rider;
import com.ridex.rider.event.RiderCreatedEvent;
import com.ridex.rider.event.UserRegisteredEvent;
import com.ridex.rider.repository.ProcessedEventRepository;
import com.ridex.rider.repository.RiderRepository;
import com.ridex.rider.service.util.OutboxEventService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiderRegistrationServiceImplTest {

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private RiderRegistrationServiceImpl registrationService;

    private UserRegisteredEvent event;

    @BeforeEach
    void setUp() {
        event = new UserRegisteredEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "John Rider",
                "RIDER",
                Instant.now());
    }

    @Test
    void handleUserRegistered_shouldCreateRiderAndPublishEvent() {
        when(processedEventRepository.existsById(event.eventId()))
                .thenReturn(false);

        when(riderRepository.existsByUserId(event.userId()))
                .thenReturn(false);

        when(riderRepository.save(any(Rider.class)))
                .thenAnswer(invocation -> {
                    Rider rider = invocation.getArgument(0);
                    rider.setId(UUID.randomUUID());
                    return rider;
                });

        registrationService.handleUserRegistered(event);

        ArgumentCaptor<Rider> riderCaptor = ArgumentCaptor.forClass(Rider.class);

        verify(riderRepository).save(riderCaptor.capture());

        Rider savedRider = riderCaptor.getValue();

        assertEquals(event.userId(), savedRider.getUserId());
        assertEquals(event.name(), savedRider.getName());

        ArgumentCaptor<ProcessedEvent> processedCaptor = ArgumentCaptor.forClass(ProcessedEvent.class);

        verify(processedEventRepository).save(processedCaptor.capture());

        assertEquals(
                event.eventId(),
                processedCaptor.getValue().getEventId());

        ArgumentCaptor<RiderCreatedEvent> createdEventCaptor = ArgumentCaptor.forClass(RiderCreatedEvent.class);

        verify(outboxEventService)
                .saveRiderCreatedEvent(createdEventCaptor.capture());

        RiderCreatedEvent createdEvent = createdEventCaptor.getValue();

        assertNotNull(createdEvent.eventId());
        assertEquals(savedRider.getId(), createdEvent.riderId());
        assertEquals(event.userId(), createdEvent.userId());
        assertNotNull(createdEvent.createdAt());
    }

    @Test
    void handleUserRegistered_shouldIgnoreAlreadyProcessedEvent() {
        when(processedEventRepository.existsById(event.eventId()))
                .thenReturn(true);

        registrationService.handleUserRegistered(event);

        verify(processedEventRepository).existsById(event.eventId());
        verifyNoInteractions(riderRepository);
        verify(processedEventRepository, never()).save(any());
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void handleUserRegistered_shouldMarkNonRiderEventAsProcessed() {
        UserRegisteredEvent driverEvent = new UserRegisteredEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "John Driver",
                "DRIVER",
                Instant.now());

        when(processedEventRepository.existsById(driverEvent.eventId()))
                .thenReturn(false);

        registrationService.handleUserRegistered(driverEvent);

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);

        verify(processedEventRepository).save(captor.capture());

        assertEquals(
                driverEvent.eventId(),
                captor.getValue().getEventId());

        verifyNoInteractions(riderRepository);
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void handleUserRegistered_shouldMarkEventProcessedWhenRiderAlreadyExists() {
        when(processedEventRepository.existsById(event.eventId()))
                .thenReturn(false);

        when(riderRepository.existsByUserId(event.userId()))
                .thenReturn(true);

        registrationService.handleUserRegistered(event);

        verify(riderRepository).existsByUserId(event.userId());
        verify(riderRepository, never()).save(any());

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);

        verify(processedEventRepository).save(captor.capture());

        assertEquals(
                event.eventId(),
                captor.getValue().getEventId());

        verifyNoInteractions(outboxEventService);
    }
}