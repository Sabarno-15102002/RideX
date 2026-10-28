package com.ridex.pricing.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridex.pricing.entity.RiderIdentity;

public interface RiderIdentityRepository extends JpaRepository<RiderIdentity, UUID> {

    boolean existsByRiderId(UUID riderId);
}