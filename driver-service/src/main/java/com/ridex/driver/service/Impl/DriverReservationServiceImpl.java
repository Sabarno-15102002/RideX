package com.ridex.driver.service.Impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.DriverReservation;
import com.ridex.driver.event.DriverRideAcceptedEvent;
import com.ridex.driver.event.DriverRideExpiredEvent;
import com.ridex.driver.event.DriverRideRejectedEvent;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.DriverReservationRepository;
import com.ridex.driver.service.DriverReservationService;
import com.ridex.driver.service.utilities.OutboxEventService;
import com.ridex.driver.utilities.DriverStatus;
import com.ridex.driver.utilities.ReservationStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverReservationServiceImpl implements DriverReservationService {

        private static final Duration RESERVATION_DURATION = Duration.ofSeconds(15);

        private final DriverRepository driverRepository;
        private final DriverReservationRepository reservationRepository;
        private final OutboxEventService outboxEventService;

        @Override
        @Transactional
        public boolean reserveDriver(
                        UUID driverId,
                        UUID tripId) {

                Driver driver = driverRepository.findById(driverId)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Driver not found: " + driverId));

                if (driver.getStatus() != DriverStatus.AVAILABLE) {
                        return false;
                }

                DriverReservation reservation = DriverReservation.builder()
                                .driverId(driverId)
                                .tripId(tripId)
                                .status(ReservationStatus.ACTIVE)
                                .expiresAt(
                                                Instant.now()
                                                                .plus(RESERVATION_DURATION))
                                .build();

                try {
                        reservationRepository.saveAndFlush(reservation);
                        return true;

                } catch (DataIntegrityViolationException e) {
                        return false;
                }
        }

        @Override
        @Transactional
        public void acceptReservation(UUID driverId, UUID tripId) {

                DriverReservation reservation = reservationRepository
                                .findByTripIdForUpdate(tripId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Reservation not found"));

                // 1. Verify ownership
                if (!reservation.getDriverId().equals(driverId)) {
                        throw new IllegalStateException(
                                        "Driver does not own this reservation");
                }

                // 2. Verify reservation state
                if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                        throw new IllegalStateException(
                                        "Reservation is not active");
                }

                // 3. Verify reservation hasn't expired
                Instant now = Instant.now();

                if (reservation.getExpiresAt().isBefore(now)) {
                        reservation.setStatus(ReservationStatus.EXPIRED);
                        reservation.setUpdatedAt(now);

                        reservationRepository.save(reservation);

                        throw new IllegalStateException(
                                        "Reservation has expired");
                }

                // 4. Load driver
                Driver driver = driverRepository.findById(driverId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Driver not found"));

                // 5. Verify driver is still AVAILABLE
                if (driver.getStatus() != DriverStatus.AVAILABLE) {
                        throw new IllegalStateException(
                                        "Driver is no longer available");
                }

                // 6. Accept reservation
                reservation.setStatus(ReservationStatus.ACCEPTED);
                reservation.setUpdatedAt(now);

                // 7. Move driver to ON_TRIP
                driver.setStatus(DriverStatus.ON_TRIP);
                driver.setUpdatedAt(now);

                // 8. Persist both in the same transaction
                reservationRepository.save(reservation);
                driverRepository.save(driver);

                DriverRideAcceptedEvent event = new DriverRideAcceptedEvent(
                                UUID.randomUUID(),
                                tripId,
                                driverId,
                                Instant.now());

                outboxEventService.saveDriverRideAcceptedEvent(event);
        }

        @Override
        @Transactional
        public void releaseReservation(UUID driverId, UUID tripId) {

                DriverReservation reservation = reservationRepository
                                .findByTripIdForUpdate(tripId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Reservation not found"));

                if (!reservation.getDriverId().equals(driverId)) {
                        throw new IllegalStateException(
                                        "Driver does not own this reservation");
                }

                if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                        return;
                }

                Instant now = Instant.now();

                reservation.setStatus(ReservationStatus.RELEASED);
                reservation.setUpdatedAt(now);

                reservationRepository.save(reservation);

                DriverRideRejectedEvent event = new DriverRideRejectedEvent(
                                UUID.randomUUID(),
                                tripId,
                                driverId,
                                now);

                outboxEventService.saveDriverRideRejectedEvent(event);
        }

        @Override
        @Transactional
        public void expireReservations() {

                Instant now = Instant.now();

                List<DriverReservation> reservations = reservationRepository.findExpiredReservations(
                                ReservationStatus.ACTIVE,
                                now,
                                PageRequest.of(0, 100));

                for (DriverReservation reservation : reservations) {

                        reservation.setStatus(ReservationStatus.EXPIRED);
                        reservation.setUpdatedAt(now);

                        reservationRepository.save(reservation);

                        log.info(
                                        "Expired driver reservation: reservationId={}, tripId={}, driverId={}",
                                        reservation.getId(),
                                        reservation.getTripId(),
                                        reservation.getDriverId());

                        DriverRideExpiredEvent event = new DriverRideExpiredEvent(
                                        UUID.randomUUID(),
                                        reservation.getTripId(),
                                        reservation.getDriverId(),
                                        now);
                        
                        outboxEventService.saveDriverRideExpiredEvent(event);

                }
        }
}