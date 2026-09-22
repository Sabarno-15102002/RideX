package com.ridex.pricing.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridex.pricing.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}