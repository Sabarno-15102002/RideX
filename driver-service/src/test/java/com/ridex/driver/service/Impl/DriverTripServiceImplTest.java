
package com.ridex.driver.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.DriverReservation;
import com.ridex.driver.exception.ResourceNotFoundException;
import com.ridex.driver.exception.TripStateException;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.service.DriverReservationService;
import com.ridex.driver.service.DriverService;
import com.ridex.driver.service.utilities.OutboxEventService;
import com.ridex.driver.utilities.DriverStatus;
import com.ridex.driver.utilities.ReservationStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverTripServiceImplTest {

        @Mock
        private OutboxEventService outboxEventService;

        @Mock
        private DriverRepository driverRepository;

        @Mock
        private DriverReservationService reservationService;

        @Mock
        private com.ridex.driver.repository.ProcessedEventRepository processedEventRepository;

        @Mock
        private DriverService driverService;

        @InjectMocks
        private DriverTripServiceImpl tripService;

        private UUID userId;
        private UUID driverId;
        private UUID tripId;

        private Driver driver;
        private DriverReservation reservation;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                driverId = UUID.randomUUID();
                tripId = UUID.randomUUID();

                driver = Driver.builder()
                                .id(driverId)
                                .userId(userId)
                                .name("John")
                                .status(DriverStatus.ON_TRIP)
                                .build();

                reservation = DriverReservation.builder()
                                .id(UUID.randomUUID())
                                .driverId(driverId)
                                .tripId(tripId)
                                .status(ReservationStatus.ACCEPTED)
                                .build();
        }

        @Test
        void acceptTrip_shouldDelegateToReservationService() {
                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                tripService.acceptTrip(userId, tripId);

                verify(reservationService).acceptReservation(driverId, tripId);
        }

        @Test
        void acceptTrip_shouldThrowWhenDriverDoesNotExist() {
                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> tripService.acceptTrip(userId, tripId));

                verifyNoInteractions(reservationService);
        }

        @Test
        void rejectTrip_shouldDelegateToReservationService() {
                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                tripService.rejectTrip(userId, tripId);

                verify(reservationService).releaseReservation(driverId, tripId);
        }

        @Test
        void rejectTrip_shouldThrowWhenDriverDoesNotExist() {
                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> tripService.rejectTrip(userId, tripId));

                verifyNoInteractions(reservationService);
        }

        @Test
        void arriveAtPickup_shouldPublishEventForAcceptedTrip() {
                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                tripService.arriveAtPickup(userId, tripId);

                verify(outboxEventService).saveDriverArrivedEvent(any());
        }

        @Test
        void arriveAtPickup_shouldRejectTripOwnedByAnotherDriver() {
                reservation.setDriverId(UUID.randomUUID());

                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                assertThrows(
                                TripStateException.class,
                                () -> tripService.arriveAtPickup(userId, tripId));

                verifyNoInteractions(outboxEventService);
        }

        @Test
        void arriveAtPickup_shouldRejectUnacceptedTrip() {
                reservation.setStatus(ReservationStatus.ACTIVE);

                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                assertThrows(
                                TripStateException.class,
                                () -> tripService.arriveAtPickup(userId, tripId));
        }

        @Test
        void arriveAtPickup_shouldRejectDriverNotOnTrip() {
                driver.setStatus(DriverStatus.AVAILABLE);

                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                assertThrows(
                                TripStateException.class,
                                () -> tripService.arriveAtPickup(userId, tripId));
        }

        @Test
        void startTrip_shouldPublishEventForAcceptedTrip() {
                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                tripService.startTrip(userId, tripId);

                verify(outboxEventService).saveDriverTripStartedEvent(any());
        }

        @Test
        void startTrip_shouldRejectTripOwnedByAnotherDriver() {
                reservation.setDriverId(UUID.randomUUID());

                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                assertThrows(
                                TripStateException.class,
                                () -> tripService.startTrip(userId, tripId));

                verifyNoInteractions(outboxEventService);
        }

        @Test
        void startTrip_shouldRejectUnacceptedTrip() {
                reservation.setStatus(ReservationStatus.ACTIVE);

                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                assertThrows(
                                TripStateException.class,
                                () -> tripService.startTrip(userId, tripId));
        }

        @Test
        void startTrip_shouldRejectDriverNotOnTrip() {
                driver.setStatus(DriverStatus.AVAILABLE);

                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                assertThrows(
                                TripStateException.class,
                                () -> tripService.startTrip(userId, tripId));
        }

        @Test
        void completeTrip_shouldPublishEventForAcceptedTrip() {
                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                tripService.completeTrip(userId, tripId);

                verify(outboxEventService).saveDriverTripCompletedEvent(any());
        }

        @Test
        void completeTrip_shouldRejectTripOwnedByAnotherDriver() {
                reservation.setDriverId(UUID.randomUUID());

                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                assertThrows(
                                TripStateException.class,
                                () -> tripService.completeTrip(userId, tripId));

                verifyNoInteractions(outboxEventService);
        }

        @Test
        void completeTrip_shouldRejectUnacceptedTrip() {
                reservation.setStatus(ReservationStatus.ACTIVE);

                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                assertThrows(
                                TripStateException.class,
                                () -> tripService.completeTrip(userId, tripId));
        }

        @Test
        void completeTrip_shouldRejectDriverNotOnTrip() {
                driver.setStatus(DriverStatus.AVAILABLE);

                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(reservationService.findDriverReservation(tripId))
                                .thenReturn(reservation);

                assertThrows(
                                TripStateException.class,
                                () -> tripService.completeTrip(userId, tripId));
        }
}
