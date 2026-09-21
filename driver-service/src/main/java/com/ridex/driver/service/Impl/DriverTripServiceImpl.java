package com.ridex.driver.service.Impl;

import com.ridex.driver.service.utilities.OutboxEventService;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.DriverReservation;
import com.ridex.driver.entity.ProcessedEvent;
import com.ridex.driver.event.DriverArrivedEvent;
import com.ridex.driver.event.DriverTripCompletedEvent;
import com.ridex.driver.event.DriverTripStartedEvent;
import com.ridex.driver.event.TripCompletedEvent;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.ProcessedEventRepository;
import com.ridex.driver.service.DriverReservationService;
import com.ridex.driver.service.DriverService;
import com.ridex.driver.service.DriverTripService;
import com.ridex.driver.utilities.DriverStatus;
import com.ridex.driver.utilities.ReservationStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DriverTripServiceImpl implements DriverTripService {

        private final OutboxEventService outboxEventService;
        private final DriverRepository driverRepository;
        private final DriverReservationService reservationService;
        private final ProcessedEventRepository processedEventRepository;
        private final DriverService driverService;

        @Override
        public void acceptTrip(UUID userId, UUID tripId) {

                Driver driver = driverRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Driver not found"));

                reservationService.acceptReservation(
                                driver.getId(),
                                tripId);
        }

        @Override
        public void rejectTrip(UUID userId, UUID tripId) {

                Driver driver = driverRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Driver not found"));

                reservationService.releaseReservation(
                                driver.getId(),
                                tripId);
        }

        @Override
        @Transactional
        public void arriveAtPickup(UUID userId, UUID tripId) {

                Driver driver = driverRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Driver not found"));

                DriverReservation reservation = reservationService.findDriverReservation(tripId);

                if (!reservation.getDriverId().equals(driver.getId())) {
                        throw new IllegalStateException(
                                        "Driver does not own this trip");
                }

                if (reservation.getStatus() != ReservationStatus.ACCEPTED) {
                        throw new IllegalStateException(
                                        "Trip has not been accepted");
                }

                if (driver.getStatus() != DriverStatus.ON_TRIP) {
                        throw new IllegalStateException(
                                        "Driver is not currently on a trip");
                }

                Instant now = Instant.now();

                DriverArrivedEvent event = new DriverArrivedEvent(
                                UUID.randomUUID(),
                                tripId,
                                driver.getId(),
                                now);

                outboxEventService.saveDriverArrivedEvent(event);
        }

        @Override
        @Transactional
        public void startTrip(UUID userId, UUID tripId) {

                Driver driver = driverRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Driver not found"));

                DriverReservation reservation = reservationService.findDriverReservation(tripId);

                if (!reservation.getDriverId().equals(driver.getId())) {
                        throw new IllegalStateException(
                                        "Driver does not own this trip");
                }

                if (reservation.getStatus() != ReservationStatus.ACCEPTED) {
                        throw new IllegalStateException(
                                        "Trip has not been accepted");
                }

                if (driver.getStatus() != DriverStatus.ON_TRIP) {
                        throw new IllegalStateException(
                                        "Driver is not on a trip");
                }

                Instant now = Instant.now();

                DriverTripStartedEvent event = new DriverTripStartedEvent(
                                UUID.randomUUID(),
                                tripId,
                                driver.getId(),
                                now);

                outboxEventService.saveDriverTripStartedEvent(event);
        }

        @Transactional
        public void completeTrip(UUID userId, UUID tripId) {

                Driver driver = driverRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Driver not found"));

                DriverReservation reservation = reservationService.findDriverReservation(tripId);

                if (!reservation.getDriverId().equals(driver.getId())) {
                        throw new IllegalStateException(
                                        "Driver does not own this trip");
                }

                if (reservation.getStatus() != ReservationStatus.ACCEPTED) {
                        throw new IllegalStateException(
                                        "Trip has not been accepted");
                }

                if (driver.getStatus() != DriverStatus.ON_TRIP) {
                        throw new IllegalStateException(
                                        "Driver is not on a trip");
                }

                Instant now = Instant.now();

                DriverTripCompletedEvent event = new DriverTripCompletedEvent(
                                UUID.randomUUID(),
                                tripId,
                                driver.getId(),
                                now);

                outboxEventService.saveDriverTripCompletedEvent(event);
        }

        @Override 
        @Transactional
        public void handleTripCompleted(TripCompletedEvent event) {

                if (processedEventRepository.existsById(event.eventId())) {
                        return;
                }

                Driver driver = driverRepository.findById(event.driverId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Driver not found"));

                if (driver.getStatus() != DriverStatus.ON_TRIP) {
                        return;
                }

                driver.setStatus(DriverStatus.AVAILABLE);
                driver.setUpdatedAt(Instant.now());

                driverRepository.save(driver);

                processedEventRepository.save(
                                new ProcessedEvent(
                                                event.eventId(),
                                                Instant.now()));

                driverService.updateStatus(driver.getId(), DriverStatus.AVAILABLE);
        }
}