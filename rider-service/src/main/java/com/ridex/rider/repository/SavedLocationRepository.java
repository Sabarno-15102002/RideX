package com.ridex.rider.repository;

import com.ridex.rider.entity.SavedLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedLocationRepository extends JpaRepository<SavedLocation, UUID> {

    List<SavedLocation> findAllByRiderId(UUID riderId);

    Optional<SavedLocation> findByIdAndRiderId(
            UUID id,
            UUID riderId
    );
}