package com.ridex.pricing.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fare_quotes")
@Getter
@Setter
@NoArgsConstructor
public class FareQuote {

    @Id
    private UUID id;

    @Column(name = "trip_id", nullable = false, unique = true)
    private UUID tripId;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "duration_minutes", nullable = false, precision = 10, scale = 2)
    private BigDecimal durationMinutes;

    @Column(name = "distance_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal distanceFare;

    @Column(name = "duration_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal durationFare;

    @Column(name = "total_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalFare;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}