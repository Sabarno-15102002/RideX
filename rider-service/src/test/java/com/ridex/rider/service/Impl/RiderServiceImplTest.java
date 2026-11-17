package com.ridex.rider.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.ridex.rider.dto.request.UpdateRiderRequest;
import com.ridex.rider.dto.response.RiderResponse;
import com.ridex.rider.entity.Rider;
import com.ridex.rider.exception.RiderNotFoundException;
import com.ridex.rider.repository.RiderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiderServiceImplTest {

    @Mock
    private RiderRepository riderRepository;

    @InjectMocks
    private RiderServiceImpl riderService;

    private UUID userId;
    private Rider rider;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        rider = Rider.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("John Rider")
                .rating(new BigDecimal("4.85"))
                .build();
    }

    @Test
    void getMyProfile_shouldReturnRiderProfile() {

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.of(rider));

        RiderResponse response = riderService.getMyProfile(userId);

        assertNotNull(response);
        assertEquals(rider.getId(), response.id());
        assertEquals(userId, response.userId());
        assertEquals("John Rider", response.name());
        assertEquals(new BigDecimal("4.85"), response.rating());

        verify(riderRepository).findByUserId(userId);
        verifyNoMoreInteractions(riderRepository);
    }

    @Test
    void getMyProfile_shouldThrowWhenRiderDoesNotExist() {

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        RiderNotFoundException exception = assertThrows(
                RiderNotFoundException.class,
                () -> riderService.getMyProfile(userId));

        assertEquals("Rider profile not found", exception.getMessage());

        verify(riderRepository).findByUserId(userId);
        verify(riderRepository, never()).save(any());
    }

    @Test
    void updateMyProfile_shouldUpdateRiderName() {

        UpdateRiderRequest request = new UpdateRiderRequest("John Updated");

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.of(rider));

        when(riderRepository.save(rider))
                .thenReturn(rider);

        RiderResponse response = riderService.updateMyProfile(userId, request);

        assertNotNull(response);
        assertEquals("John Updated", response.name());
        assertEquals(rider.getId(), response.id());
        assertEquals(userId, response.userId());
        assertEquals(new BigDecimal("4.85"), response.rating());

        assertEquals("John Updated", rider.getName());

        verify(riderRepository).findByUserId(userId);
        verify(riderRepository).save(rider);
        verifyNoMoreInteractions(riderRepository);
    }

    @Test
    void updateMyProfile_shouldThrowWhenRiderDoesNotExist() {

        UpdateRiderRequest request = new UpdateRiderRequest("John Updated");

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        RiderNotFoundException exception = assertThrows(
                RiderNotFoundException.class,
                () -> riderService.updateMyProfile(userId, request));

        assertEquals("Rider profile not found", exception.getMessage());

        verify(riderRepository).findByUserId(userId);
        verify(riderRepository, never()).save(any());
    }
}
