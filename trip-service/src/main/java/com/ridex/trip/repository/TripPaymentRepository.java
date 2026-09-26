package com.ridex.trip.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridex.trip.entity.TripPayment;

public interface TripPaymentRepository extends JpaRepository<TripPayment, UUID> {

    Optional<TripPayment> findByPaymentId(UUID paymentId);
}