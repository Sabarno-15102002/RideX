package com.ridex.driver.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridex.driver.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsById(UUID eventId);
}