package com.ridex.trip.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridex.trip.entity.Trip;

public interface TripRepository extends JpaRepository<Trip, UUID> {
        
}