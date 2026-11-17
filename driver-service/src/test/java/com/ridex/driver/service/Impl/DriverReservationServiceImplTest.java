package com.ridex.driver.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.DriverReservation;
import com.ridex.driver.event.DriverRideAcceptedEvent;
import com.ridex.driver.event.DriverRideExpiredEvent;
import com.ridex.driver.event.DriverRideRejectedEvent;
import com.ridex.driver.exception.ResourceNotFoundException;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.DriverReservationRepository;
import com.ridex.driver.service.utilities.OutboxEventService;
import com.ridex.driver.utilities.DriverStatus;
import com.ridex.driver.utilities.ReservationStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverReservationServiceImplTest {

        @Mock
        private DriverRepository driverRepository;

        @Mock
        private DriverReservationRepository reservationRepository;

        @Mock
        private OutboxEventService outboxEventService;

        @InjectMocks
        private DriverReservationServiceImpl reservationService;

        private UUID driverId;
        private UUID otherDriverId;
        private UUID tripId;

        private Driver driver;
        private DriverReservation reservation;

        @BeforeEach
        void setUp() {
                driverId = UUID.randomUUID();
                otherDriverId = UUID.randomUUID();
                tripId = UUID.randomUUID();

                driver = Driver.builder()
                                .id(driverId)
                                .userId(UUID.randomUUID())
                                .name("John")
                                .licenseNumber("DL12345")
                                .rating(BigDecimal.valueOf(5.00))
                                .status(DriverStatus.AVAILABLE)
                                .build();

                reservation = createReservation(
                                driverId,
                                tripId,
                                ReservationStatus.ACTIVE,
                                Instant.now().plusSeconds(15));
        }

        // --------------------------------------------------
        // reserveDriver
        // --------------------------------------------------

        @Test
        void reserveDriver_shouldReturnTrueWhenSameDriverAlreadyReservedTrip() {
                when(reservationRepository.findByTripIdAndStatus(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.of(reservation));

                boolean result = reservationService.reserveDriver(driverId, tripId);

                assertTrue(result);

                verifyNoInteractions(driverRepository);
                verify(reservationRepository, never())
                                .tryCreateActiveReservation(any(), any(), any(), any());
        }

        @Test
        void reserveDriver_shouldReturnFalseWhenAnotherDriverReservedTrip() {
                DriverReservation existing = createReservation(
                                otherDriverId,
                                tripId,
                                ReservationStatus.ACTIVE,
                                Instant.now().plusSeconds(15));

                when(reservationRepository.findByTripIdAndStatus(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.of(existing));

                boolean result = reservationService.reserveDriver(driverId, tripId);

                assertFalse(result);

                verifyNoInteractions(driverRepository);
        }

        @Test
        void reserveDriver_shouldThrowWhenDriverDoesNotExist() {
                when(reservationRepository.findByTripIdAndStatus(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.empty());

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> reservationService.reserveDriver(driverId, tripId));
        }

        @Test
        void reserveDriver_shouldReturnFalseWhenDriverIsOffline() {
                driver.setStatus(DriverStatus.OFFLINE);

                when(reservationRepository.findByTripIdAndStatus(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.empty());

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                boolean result = reservationService.reserveDriver(driverId, tripId);

                assertFalse(result);

                verify(reservationRepository, never())
                                .tryCreateActiveReservation(any(), any(), any(), any());
        }

        @Test
        void reserveDriver_shouldReturnFalseWhenDriverIsAlreadyOnTrip() {
                driver.setStatus(DriverStatus.ON_TRIP);

                when(reservationRepository.findByTripIdAndStatus(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.empty());

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                boolean result = reservationService.reserveDriver(driverId, tripId);

                assertFalse(result);
        }

        @Test
        void reserveDriver_shouldCreateReservationSuccessfully() {
                when(reservationRepository.findByTripIdAndStatus(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.empty());

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                when(reservationRepository.tryCreateActiveReservation(
                                any(UUID.class),
                                eq(driverId),
                                eq(tripId),
                                any(Instant.class))).thenReturn(1);

                boolean result = reservationService.reserveDriver(driverId, tripId);

                assertTrue(result);

                ArgumentCaptor<Instant> expiryCaptor = ArgumentCaptor.forClass(Instant.class);

                verify(reservationRepository).tryCreateActiveReservation(
                                any(UUID.class),
                                eq(driverId),
                                eq(tripId),
                                expiryCaptor.capture());

                Instant expiry = expiryCaptor.getValue();

                assertTrue(expiry.isAfter(Instant.now()));
                assertTrue(expiry.isBefore(Instant.now().plusSeconds(16)));
        }

        @Test
        void reserveDriver_shouldReturnTrueWhenConcurrentWinnerIsSameDriver() {
                when(reservationRepository.findByTripIdAndStatus(
                                tripId,
                                ReservationStatus.ACTIVE))
                                .thenReturn(Optional.empty())
                                .thenReturn(Optional.of(reservation));

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                when(reservationRepository.tryCreateActiveReservation(
                                any(UUID.class),
                                eq(driverId),
                                eq(tripId),
                                any(Instant.class))).thenReturn(0);

                boolean result = reservationService.reserveDriver(driverId, tripId);

                assertTrue(result);

                verify(reservationRepository, times(2))
                                .findByTripIdAndStatus(tripId, ReservationStatus.ACTIVE);
        }

        @Test
        void reserveDriver_shouldReturnFalseWhenConcurrentWinnerIsAnotherDriver() {
                DriverReservation winner = createReservation(
                                otherDriverId,
                                tripId,
                                ReservationStatus.ACTIVE,
                                Instant.now().plusSeconds(15));

                when(reservationRepository.findByTripIdAndStatus(
                                tripId,
                                ReservationStatus.ACTIVE))
                                .thenReturn(Optional.empty())
                                .thenReturn(Optional.of(winner));

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                when(reservationRepository.tryCreateActiveReservation(
                                any(UUID.class),
                                eq(driverId),
                                eq(tripId),
                                any(Instant.class))).thenReturn(0);

                boolean result = reservationService.reserveDriver(driverId, tripId);

                assertFalse(result);
        }

        @Test
        void reserveDriver_shouldReturnFalseWhenConcurrentWinnerCannotBeFound() {
                when(reservationRepository.findByTripIdAndStatus(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.empty());

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                when(reservationRepository.tryCreateActiveReservation(
                                any(UUID.class),
                                eq(driverId),
                                eq(tripId),
                                any(Instant.class))).thenReturn(0);

                boolean result = reservationService.reserveDriver(driverId, tripId);

                assertFalse(result);
        }

        // --------------------------------------------------
        // acceptReservation
        // --------------------------------------------------

        @Test
        void acceptReservation_shouldAcceptReservationAndMoveDriverOnTrip() {
                when(reservationRepository.findByTripIdAndStatusForUpdate(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.of(reservation));

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                reservationService.acceptReservation(driverId, tripId);

                assertEquals(ReservationStatus.ACCEPTED, reservation.getStatus());
                assertEquals(DriverStatus.ON_TRIP, driver.getStatus());

                verify(reservationRepository).save(reservation);
                verify(driverRepository).save(driver);

                verify(outboxEventService)
                                .saveDriverRideAcceptedEvent(
                                                any(DriverRideAcceptedEvent.class));
        }

        @Test
        void acceptReservation_shouldThrowWhenActiveReservationDoesNotExist() {
                when(reservationRepository.findByTripIdAndStatusForUpdate(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> reservationService.acceptReservation(driverId, tripId));

                verifyNoInteractions(driverRepository, outboxEventService);
        }

        @Test
        void acceptReservation_shouldRejectAnotherDriversReservation() {
                reservation.setDriverId(otherDriverId);

                when(reservationRepository.findByTripIdAndStatusForUpdate(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.of(reservation));

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> reservationService.acceptReservation(driverId, tripId));

                verifyNoInteractions(driverRepository, outboxEventService);
                verify(reservationRepository, never()).save(any());
        }

        @Test
        void acceptReservation_shouldMarkExpiredReservationAndThrow() {
                reservation.setExpiresAt(Instant.now().minusSeconds(10));

                when(reservationRepository.findByTripIdAndStatusForUpdate(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.of(reservation));

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> reservationService.acceptReservation(driverId, tripId));

                assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());

                verify(reservationRepository).save(reservation);
                verifyNoInteractions(driverRepository, outboxEventService);
        }

        @Test
        void acceptReservation_shouldThrowWhenDriverDoesNotExist() {
                when(reservationRepository.findByTripIdAndStatusForUpdate(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.of(reservation));

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> reservationService.acceptReservation(driverId, tripId));

                assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());

                verify(reservationRepository, never()).save(any());
                verifyNoInteractions(outboxEventService);
        }

        @Test
        void acceptReservation_shouldRejectDriverNoLongerAvailable() {
                driver.setStatus(DriverStatus.ON_TRIP);

                when(reservationRepository.findByTripIdAndStatusForUpdate(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.of(reservation));

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> reservationService.acceptReservation(driverId, tripId));

                assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());

                verify(reservationRepository, never()).save(any());
                verify(driverRepository, never()).save(any());
                verifyNoInteractions(outboxEventService);
        }

        // --------------------------------------------------
        // releaseReservation
        // --------------------------------------------------

        @Test
        void releaseReservation_shouldReleaseReservationAndPublishEvent() {
                when(reservationRepository.findByTripIdAndStatusForUpdate(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.of(reservation));

                reservationService.releaseReservation(driverId, tripId);

                assertEquals(ReservationStatus.RELEASED, reservation.getStatus());

                verify(reservationRepository).save(reservation);

                verify(outboxEventService)
                                .saveDriverRideRejectedEvent(
                                                any(DriverRideRejectedEvent.class));
        }

        @Test
        void releaseReservation_shouldThrowWhenReservationDoesNotExist() {
                when(reservationRepository.findByTripIdAndStatusForUpdate(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> reservationService.releaseReservation(driverId, tripId));

                verifyNoInteractions(outboxEventService);
        }

        @Test
        void releaseReservation_shouldRejectAnotherDriversReservation() {
                reservation.setDriverId(otherDriverId);

                when(reservationRepository.findByTripIdAndStatusForUpdate(
                                tripId,
                                ReservationStatus.ACTIVE)).thenReturn(Optional.of(reservation));

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> reservationService.releaseReservation(driverId, tripId));

                verify(reservationRepository, never()).save(any());
                verifyNoInteractions(outboxEventService);
        }

        // --------------------------------------------------
        // expireReservations
        // --------------------------------------------------

        @Test
        void expireReservations_shouldExpireAllReturnedReservations() {
                DriverReservation first = createReservation(
                                driverId,
                                UUID.randomUUID(),
                                ReservationStatus.ACTIVE,
                                Instant.now().minusSeconds(20));

                DriverReservation second = createReservation(
                                otherDriverId,
                                UUID.randomUUID(),
                                ReservationStatus.ACTIVE,
                                Instant.now().minusSeconds(10));

                when(reservationRepository.findExpiredReservations(
                                eq(ReservationStatus.ACTIVE),
                                any(Instant.class),
                                any())).thenReturn(List.of(first, second));

                reservationService.expireReservations();

                assertEquals(ReservationStatus.EXPIRED, first.getStatus());
                assertEquals(ReservationStatus.EXPIRED, second.getStatus());

                verify(reservationRepository).save(first);
                verify(reservationRepository).save(second);

                verify(outboxEventService, times(2))
                                .saveDriverRideExpiredEvent(
                                                any(DriverRideExpiredEvent.class));
        }

        @Test
        void expireReservations_shouldDoNothingWhenNoExpiredReservationsExist() {
                when(reservationRepository.findExpiredReservations(
                                eq(ReservationStatus.ACTIVE),
                                any(Instant.class),
                                any())).thenReturn(List.of());

                reservationService.expireReservations();

                verify(reservationRepository, never()).save(any());
                verifyNoInteractions(outboxEventService);
        }

        // --------------------------------------------------
        // findDriverReservation
        // --------------------------------------------------

        @Test
        void findDriverReservation_shouldReturnReservation() {
                when(reservationRepository.findByTripId(tripId))
                                .thenReturn(Optional.of(reservation));

                DriverReservation result = reservationService.findDriverReservation(tripId);

                assertSame(reservation, result);
        }

        @Test
        void findDriverReservation_shouldThrowWhenReservationDoesNotExist() {
                when(reservationRepository.findByTripId(tripId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> reservationService.findDriverReservation(tripId));
        }

        private DriverReservation createReservation(
                        UUID reservationDriverId,
                        UUID reservationTripId,
                        ReservationStatus status,
                        Instant expiresAt) {
                return DriverReservation.builder()
                                .id(UUID.randomUUID())
                                .driverId(reservationDriverId)
                                .tripId(reservationTripId)
                                .status(status)
                                .expiresAt(expiresAt)
                                .build();
        }
}
