package com.ridex.driver.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.ProcessedEvent;
import com.ridex.driver.event.TripCompletedEvent;
import com.ridex.driver.exception.ResourceNotFoundException;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.ProcessedEventRepository;
import com.ridex.driver.service.DriverReservationService;
import com.ridex.driver.service.DriverService;
import com.ridex.driver.service.utilities.OutboxEventService;
import com.ridex.driver.utilities.DriverStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverTripCompletedHandlerTest {

        @Mock
        private OutboxEventService outboxEventService;

        @Mock
        private DriverRepository driverRepository;

        @Mock
        private DriverReservationService reservationService;

        @Mock
        private ProcessedEventRepository processedEventRepository;

        @Mock
        private DriverService driverService;

        @InjectMocks
        private DriverTripServiceImpl tripService;

        private UUID eventId;
        private UUID driverId;
        private TripCompletedEvent event;
        private Driver driver;

        @BeforeEach
        void setUp() {
                eventId = UUID.randomUUID();
                driverId = UUID.randomUUID();

                event = new TripCompletedEvent(
                                eventId,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                driverId,
                                12.9716,
                                77.5946,
                                12.9352,
                                77.6245,
                                Instant.now().minusSeconds(1800),
                                Instant.now());

                driver = Driver.builder()
                                .id(driverId)
                                .userId(UUID.randomUUID())
                                .name("John")
                                .licenseNumber("DL12345")
                                .status(DriverStatus.ON_TRIP)
                                .build();
        }

        @Test
        void handleTripCompleted_shouldIgnoreAlreadyProcessedEvent() {
                when(processedEventRepository.existsById(eventId))
                                .thenReturn(true);

                tripService.handleTripCompleted(event);

                verifyNoInteractions(driverRepository);
                verifyNoInteractions(driverService);
                verify(processedEventRepository, never()).save(any());
        }

        @Test
        void handleTripCompleted_shouldThrowWhenDriverDoesNotExist() {
                when(processedEventRepository.existsById(eventId))
                                .thenReturn(false);

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> tripService.handleTripCompleted(event));

                verify(processedEventRepository, never()).save(any());
                verifyNoInteractions(driverService);
        }

        @Test
        void handleTripCompleted_shouldReturnWhenDriverIsNotOnTrip() {
                driver.setStatus(DriverStatus.AVAILABLE);

                when(processedEventRepository.existsById(eventId))
                                .thenReturn(false);

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                tripService.handleTripCompleted(event);

                verify(driverRepository, never()).save(any());
                verify(processedEventRepository, never()).save(any());
                verifyNoInteractions(driverService);
        }

        @Test
        void handleTripCompleted_shouldMakeDriverAvailableAndMarkEventProcessed() {
                when(processedEventRepository.existsById(eventId))
                                .thenReturn(false);

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                tripService.handleTripCompleted(event);

                assertEquals(DriverStatus.AVAILABLE, driver.getStatus());

                verify(driverRepository).save(driver);

                verify(processedEventRepository).save(argThat(
                                (ProcessedEvent processed) -> processed.getEventId().equals(eventId)
                                                && processed.getProcessedAt() != null));

                verify(driverService).updateStatus(
                                driverId,
                                DriverStatus.AVAILABLE);
        }

        @Test
        void handleTripCompleted_shouldNotProcessSameEventTwice() {
                when(processedEventRepository.existsById(eventId))
                                .thenReturn(false)
                                .thenReturn(true);

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                tripService.handleTripCompleted(event);
                tripService.handleTripCompleted(event);

                verify(driverRepository, times(1)).save(driver);
                verify(processedEventRepository, times(1)).save(any());
                verify(driverService, times(1)).updateStatus(
                                driverId,
                                DriverStatus.AVAILABLE);
        }
}
