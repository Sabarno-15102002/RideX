package com.ridex.driver.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridex.driver.entity.DriverReservation;
import com.ridex.driver.utilities.ReservationStatus;

public interface DriverReservationRepository extends JpaRepository<DriverReservation, UUID> {

    boolean existsByDriverIdAndStatus(
            UUID driverId,
            ReservationStatus status
    );
}