
package com.ridex.trip.service.Impl;

import com.ridex.trip.entity.ProcessedEvent;
import com.ridex.trip.entity.RiderIdentity;
import com.ridex.trip.event.event.RiderCreatedEvent;
import com.ridex.trip.repository.ProcessedEventRepository;
import com.ridex.trip.repository.RiderIdentityRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiderIdentityServiceImplTest {

    @Mock
    private RiderIdentityRepository riderIdentityRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private RiderIdentityServiceImpl riderIdentityService;

    private UUID eventId;
    private UUID userId;
    private UUID riderId;
    private Instant createdAt;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();
        riderId = UUID.randomUUID();
        createdAt = Instant.parse("2026-11-10T10:00:00Z");
    }

    private RiderCreatedEvent createEvent() {
        return new RiderCreatedEvent(
                eventId,
                riderId,
                userId,
                createdAt
        );
    }

    // ---------------------------------------------
    // HANDLE RIDER CREATED
    // ---------------------------------------------

    @Test
    void handleRiderCreated_shouldCreateIdentityAndRecordEvent() {

        RiderCreatedEvent event = createEvent();

        when(processedEventRepository.existsById(eventId))
                .thenReturn(false);

        when(riderIdentityRepository.existsById(userId))
                .thenReturn(false);

        riderIdentityService.handleRiderCreated(event);

        ArgumentCaptor<RiderIdentity> identityCaptor =
                ArgumentCaptor.forClass(RiderIdentity.class);

        verify(riderIdentityRepository).save(identityCaptor.capture());

        RiderIdentity savedIdentity = identityCaptor.getValue();

        assertEquals(userId, savedIdentity.getUserId());
        assertEquals(riderId, savedIdentity.getRiderId());

        verify(processedEventRepository).save(
                argThat(processedEvent ->
                        processedEvent.getEventId().equals(eventId)
                )
        );
    }

    @Test
    void handleRiderCreated_shouldIgnoreDuplicateEvent() {

        RiderCreatedEvent event = createEvent();

        when(processedEventRepository.existsById(eventId))
                .thenReturn(true);

        riderIdentityService.handleRiderCreated(event);

        verifyNoInteractions(riderIdentityRepository);

        verify(processedEventRepository, never())
                .save(any(ProcessedEvent.class));
    }

    @Test
    void handleRiderCreated_shouldNotCreateDuplicateIdentity() {

        RiderCreatedEvent event = createEvent();

        when(processedEventRepository.existsById(eventId))
                .thenReturn(false);

        when(riderIdentityRepository.existsById(userId))
                .thenReturn(true);

        riderIdentityService.handleRiderCreated(event);

        verify(riderIdentityRepository, never())
                .save(any(RiderIdentity.class));

        // Even when the identity already exists,
        // the event must be marked as processed.
        verify(processedEventRepository).save(
                argThat(processedEvent ->
                        processedEvent.getEventId().equals(eventId)
                )
        );
    }

    // ---------------------------------------------
    // GET RIDER ID BY USER ID
    // ---------------------------------------------

    @Test
    void getRiderIdByUserId_shouldReturnRiderId() {

        RiderIdentity identity = RiderIdentity.builder()
                .userId(userId)
                .riderId(riderId)
                .build();

        when(riderIdentityRepository.findById(userId))
                .thenReturn(Optional.of(identity));

        UUID result =
                riderIdentityService.getRiderIdByUserId(userId);

        assertEquals(riderId, result);

        verify(riderIdentityRepository).findById(userId);
    }

    @Test
    void getRiderIdByUserId_shouldThrowWhenIdentityDoesNotExist() {

        when(riderIdentityRepository.findById(userId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> riderIdentityService.getRiderIdByUserId(userId)
        );

        assertTrue(
                exception.getMessage().contains(userId.toString())
        );

        verify(riderIdentityRepository).findById(userId);
    }
}
