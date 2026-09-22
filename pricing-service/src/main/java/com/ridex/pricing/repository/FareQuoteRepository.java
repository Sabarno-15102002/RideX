package com.ridex.pricing.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridex.pricing.entity.FareQuote;

public interface FareQuoteRepository extends JpaRepository<FareQuote, UUID> {

    Optional<FareQuote> findByTripId(UUID tripId);

    boolean existsByTripId(UUID tripId);

}
