package com.ridex.pricing.service.Impl;

import com.ridex.pricing.entity.ProcessedEvent;
import com.ridex.pricing.entity.RiderIdentity;
import com.ridex.pricing.event.RiderCreatedEvent;
import com.ridex.pricing.repository.ProcessedEventRepository;
import com.ridex.pricing.repository.RiderIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RiderIdentityServiceImplTest {

    private RiderIdentityRepository riderIdentityRepository;
    private ProcessedEventRepository processedEventRepository;
    private RiderIdentityServiceImpl service;

    private UUID eventId;
    private UUID userId;
    private UUID riderId;

    @BeforeEach
    void setUp() {
        riderIdentityRepository = mock(RiderIdentityRepository.class);
        processedEventRepository = mock(ProcessedEventRepository.class);

        service = new RiderIdentityServiceImpl(
                riderIdentityRepository,
                processedEventRepository);

        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();
        riderId = UUID.randomUUID();
    }

    private RiderCreatedEvent event() {
        return new RiderCreatedEvent(
                eventId,
                riderId,
                userId,
                Instant.now());
    }

    @Test
    void shouldCreateRiderIdentityAndMarkEventProcessed() {
        when(processedEventRepository.existsById(eventId))
                .thenReturn(false);
        when(riderIdentityRepository.existsById(userId))
                .thenReturn(false);

        service.handleRiderCreated(event());

        verify(riderIdentityRepository).save(argThat(identity -> identity.getUserId().equals(userId)
                && identity.getRiderId().equals(riderId)));

        verify(processedEventRepository).save(argThat(processed -> processed.getEventId().equals(eventId)));
    }

    @Test
    void shouldIgnoreAlreadyProcessedEvent() {
        when(processedEventRepository.existsById(eventId))
                .thenReturn(true);

        service.handleRiderCreated(event());

        verifyNoInteractions(riderIdentityRepository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldNotCreateDuplicateIdentityButMarkEventProcessed() {
        when(processedEventRepository.existsById(eventId))
                .thenReturn(false);
        when(riderIdentityRepository.existsById(userId))
                .thenReturn(true);

        service.handleRiderCreated(event());

        verify(riderIdentityRepository, never()).save(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldReturnRiderIdForUser() {
        RiderIdentity identity = RiderIdentity.builder()
                .userId(userId)
                .riderId(riderId)
                .build();

        when(riderIdentityRepository.findById(userId))
                .thenReturn(Optional.of(identity));

        assertEquals(riderId, service.getRiderIdByUserId(userId));
    }

    @Test
    void shouldThrowWhenUserMappingDoesNotExist() {
        when(riderIdentityRepository.findById(userId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.getRiderIdByUserId(userId));

        assertTrue(exception.getMessage().contains(userId.toString()));
    }
}
