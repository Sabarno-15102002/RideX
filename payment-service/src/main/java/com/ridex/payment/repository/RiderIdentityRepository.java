package com.ridex.payment.repository;

import com.ridex.payment.entity.RiderIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiderIdentityRepository extends JpaRepository<RiderIdentity, UUID> {

    Optional<RiderIdentity> findByUserId(UUID userId);

    Optional<RiderIdentity> findByRiderId(UUID riderId);
}