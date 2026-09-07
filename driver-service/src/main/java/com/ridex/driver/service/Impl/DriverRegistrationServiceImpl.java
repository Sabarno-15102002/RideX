package com.ridex.driver.service.Impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.ProcessedEvent;
import com.ridex.driver.event.UserRegisteredEvent;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.ProcessedEventRepository;
import com.ridex.driver.service.DriverRegistrationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DriverRegistrationServiceImpl implements DriverRegistrationService {

    private final DriverRepository driverRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    @Transactional
    public void handleUserRegistered(UserRegisteredEvent event) {

        // Idempotency check
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        // Ignore registrations that aren't for drivers
        if (!"DRIVER".equals(event.role())) {

            processedEventRepository.save(
                    ProcessedEvent.builder()
                            .eventId(event.eventId())
                            .build()
            );

            return;
        }

        // Defensive duplicate check
        if (driverRepository.existsByUserId(event.userId())) {

            processedEventRepository.save(
                    ProcessedEvent.builder()
                            .eventId(event.eventId())
                            .build()
            );

            return;
        }

        Driver driver = Driver.builder()
                .userId(event.userId())
                .name(event.name())
                .build();

        driverRepository.save(driver);

        // Mark event processed in the same transaction
        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(event.eventId())
                        .build()
        );
    }
}