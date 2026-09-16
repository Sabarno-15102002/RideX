package com.ridex.driver.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ridex.driver.entity.DriverReservation;
import com.ridex.driver.utilities.ReservationStatus;

import jakarta.persistence.LockModeType;

public interface DriverReservationRepository extends JpaRepository<DriverReservation, UUID> {

    boolean existsByDriverIdAndStatus(UUID driverId, ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT r
                FROM DriverReservation r
                WHERE r.tripId = :tripId
            """)
    Optional<DriverReservation> findByTripIdForUpdate(@Param("tripId") UUID tripId);

    @Query("""
                SELECT r
                FROM DriverReservation r
                WHERE r.status = :status
                  AND r.expiresAt <= :now
                ORDER BY r.expiresAt ASC
            """)
    List<DriverReservation> findExpiredReservations(
            @Param("status") ReservationStatus status,
            @Param("now") Instant now,
            Pageable pageable);
}