package com.ridex.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rider_identities")
@Getter
@Setter
@NoArgsConstructor
public class RiderIdentity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "rider_id", nullable = false, unique = true)
    private UUID riderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RiderIdentity(
            UUID userId,
            UUID riderId,
            Instant createdAt) {

        this.userId = userId;
        this.riderId = riderId;
        this.createdAt = createdAt;
    }
}