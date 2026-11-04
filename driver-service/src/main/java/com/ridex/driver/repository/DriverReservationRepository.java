package com.ridex.driver.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ridex.driver.entity.DriverReservation;
import com.ridex.driver.utilities.ReservationStatus;

import jakarta.persistence.LockModeType;

public interface DriverReservationRepository extends JpaRepository<DriverReservation, UUID> {

    boolean existsByDriverIdAndStatus(UUID driverId, ReservationStatus status);

    Optional<DriverReservation> findByTripIdAndStatus(
            UUID tripId,
            ReservationStatus status);

    Optional<DriverReservation> findByTripId(UUID tripId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM DriverReservation r
            WHERE r.tripId = :tripId
              AND r.status = :status
            """)
    Optional<DriverReservation> findByTripIdAndStatusForUpdate(
            @Param("tripId") UUID tripId,
            @Param("status") ReservationStatus status);

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

    @Modifying
    @Query(value = """
            INSERT INTO driver_reservations (
                id,
                driver_id,
                trip_id,
                status,
                expires_at,
                created_at,
                updated_at
            )
            VALUES (
                :id,
                :driverId,
                :tripId,
                'ACTIVE',
                :expiresAt,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int tryCreateActiveReservation(
            @Param("id") UUID id,
            @Param("driverId") UUID driverId,
            @Param("tripId") UUID tripId,
            @Param("expiresAt") Instant expiresAt);
}