package com.ridex.trip.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridex.trip.entity.RiderIdentity;

public interface RiderIdentityRepository extends JpaRepository<RiderIdentity, UUID> {

    boolean existsByRiderId(UUID riderId);
}