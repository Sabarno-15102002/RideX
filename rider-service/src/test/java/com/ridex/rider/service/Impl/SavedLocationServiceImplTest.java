package com.ridex.rider.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ridex.rider.dto.request.CreateSavedLocationRequest;
import com.ridex.rider.dto.response.SavedLocationResponse;
import com.ridex.rider.entity.Rider;
import com.ridex.rider.entity.SavedLocation;
import com.ridex.rider.exception.RiderNotFoundException;
import com.ridex.rider.exception.SavedLocationNotFoundException;
import com.ridex.rider.repository.RiderRepository;
import com.ridex.rider.repository.SavedLocationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavedLocationServiceImplTest {

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private SavedLocationRepository savedLocationRepository;

    @InjectMocks
    private SavedLocationServiceImpl savedLocationService;

    private UUID userId;
    private UUID riderId;
    private Rider rider;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        riderId = UUID.randomUUID();

        rider = Rider.builder()
                .id(riderId)
                .userId(userId)
                .name("John Rider")
                .build();
    }

    @Test
    void create_shouldSaveLocationForAuthenticatedRider() {
        CreateSavedLocationRequest request = new CreateSavedLocationRequest(
                "Home", 12.9716, 77.5946, "Bengaluru");

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.of(rider));

        when(savedLocationRepository.save(any(SavedLocation.class)))
                .thenAnswer(invocation -> {
                    SavedLocation location = invocation.getArgument(0);
                    location.setId(UUID.randomUUID());
                    location.setCreatedAt(Instant.now());
                    return location;
                });

        SavedLocationResponse response = savedLocationService.create(userId, request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals("Home", response.label());
        assertEquals(12.9716, response.latitude());
        assertEquals(77.5946, response.longitude());
        assertEquals("Bengaluru", response.address());
        assertNotNull(response.createdAt());

        ArgumentCaptor<SavedLocation> captor = ArgumentCaptor.forClass(SavedLocation.class);

        verify(savedLocationRepository).save(captor.capture());

        assertEquals(riderId, captor.getValue().getRiderId());
        assertEquals("Home", captor.getValue().getLabel());
    }

    @Test
    void create_shouldThrowWhenRiderDoesNotExist() {
        CreateSavedLocationRequest request = new CreateSavedLocationRequest(
                "Home", 12.9716, 77.5946, "Bengaluru");

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                RiderNotFoundException.class,
                () -> savedLocationService.create(userId, request));

        verify(savedLocationRepository, never()).save(any());
    }

    @Test
    void getAll_shouldReturnAllLocationsForRider() {
        Instant createdAt = Instant.now();

        SavedLocation home = SavedLocation.builder()
                .id(UUID.randomUUID())
                .riderId(riderId)
                .label("Home")
                .latitude(12.9716)
                .longitude(77.5946)
                .address("Bengaluru")
                .createdAt(createdAt)
                .build();

        SavedLocation work = SavedLocation.builder()
                .id(UUID.randomUUID())
                .riderId(riderId)
                .label("Work")
                .latitude(12.9352)
                .longitude(77.6245)
                .address("Bengaluru")
                .createdAt(createdAt)
                .build();

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.of(rider));

        when(savedLocationRepository.findAllByRiderId(riderId))
                .thenReturn(List.of(home, work));

        List<SavedLocationResponse> responses = savedLocationService.getAll(userId);

        assertEquals(2, responses.size());
        assertEquals("Home", responses.get(0).label());
        assertEquals("Work", responses.get(1).label());

        verify(savedLocationRepository).findAllByRiderId(riderId);
    }

    @Test
    void getAll_shouldReturnEmptyListWhenNoLocationsExist() {
        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.of(rider));

        when(savedLocationRepository.findAllByRiderId(riderId))
                .thenReturn(List.of());

        List<SavedLocationResponse> responses = savedLocationService.getAll(userId);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void getAll_shouldThrowWhenRiderDoesNotExist() {
        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                RiderNotFoundException.class,
                () -> savedLocationService.getAll(userId));

        verify(savedLocationRepository, never())
                .findAllByRiderId(any());
    }

    @Test
    void delete_shouldDeleteLocationOwnedByRider() {
        UUID locationId = UUID.randomUUID();

        SavedLocation location = SavedLocation.builder()
                .id(locationId)
                .riderId(riderId)
                .label("Home")
                .latitude(12.9716)
                .longitude(77.5946)
                .build();

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.of(rider));

        when(savedLocationRepository.findByIdAndRiderId(
                locationId, riderId))
                .thenReturn(Optional.of(location));

        assertDoesNotThrow(
                () -> savedLocationService.delete(userId, locationId));

        verify(savedLocationRepository).delete(location);
    }

    @Test
    void delete_shouldNotDeleteLocationOwnedByAnotherRider() {
        UUID locationId = UUID.randomUUID();

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.of(rider));

        when(savedLocationRepository.findByIdAndRiderId(
                locationId, riderId))
                .thenReturn(Optional.empty());

        assertThrows(
                SavedLocationNotFoundException.class,
                () -> savedLocationService.delete(userId, locationId));

        verify(savedLocationRepository, never()).delete(any());
    }

    @Test
    void delete_shouldThrowWhenRiderDoesNotExist() {
        UUID locationId = UUID.randomUUID();

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                RiderNotFoundException.class,
                () -> savedLocationService.delete(userId, locationId));

        verify(savedLocationRepository, never())
                .findByIdAndRiderId(any(), any());

        verify(savedLocationRepository, never()).delete(any());
    }
}