package com.ridex.rider.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridex.rider.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsById(UUID eventId);
}