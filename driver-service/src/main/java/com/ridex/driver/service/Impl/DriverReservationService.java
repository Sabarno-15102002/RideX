package com.ridex.driver.service.Impl;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.DriverReservation;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.DriverReservationRepository;
import com.ridex.driver.utilities.DriverStatus;
import com.ridex.driver.utilities.ReservationStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DriverReservationService {

    private static final Duration RESERVATION_DURATION = Duration.ofSeconds(15);

    private final DriverRepository driverRepository;
    private final DriverReservationRepository reservationRepository;

    @Transactional
    public boolean reserveDriver(
            UUID driverId,
            UUID tripId
    ) {

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Driver not found: " + driverId
                        ));

        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            return false;
        }

        DriverReservation reservation =
                DriverReservation.builder()
                        .driverId(driverId)
                        .tripId(tripId)
                        .status(ReservationStatus.ACTIVE)
                        .expiresAt(
                                Instant.now()
                                        .plus(RESERVATION_DURATION)
                        )
                        .build();

        try {
            reservationRepository.saveAndFlush(reservation);
            return true;

        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}