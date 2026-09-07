package com.ridex.driver.service.Impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.driver.dto.request.DriverOnboardingRequest;
import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.Vehicle;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.VehicleRepository;
import com.ridex.driver.service.DriverOnboardingService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DriverOnboardingServiceImpl implements DriverOnboardingService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public void onboard(
            UUID driverId,
            DriverOnboardingRequest request
    ) {

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Driver not found: " + driverId
                        )
                );

        if (driver.getLicenseNumber() != null) {
            throw new IllegalStateException(
                    "Driver onboarding is already completed"
            );
        }

        if (vehicleRepository.existsByRegistrationNumber(
                request.vehicle().registrationNumber())) {

            throw new IllegalStateException(
                    "Vehicle registration number already exists"
            );
        }

        driver.setLicenseNumber(request.licenseNumber());

        Vehicle vehicle = Vehicle.builder()
                .driverId(driver.getId())
                .registrationNumber(
                        request.vehicle().registrationNumber()
                )
                .make(request.vehicle().make())
                .model(request.vehicle().model())
                .color(request.vehicle().color())
                .vehicleType(request.vehicle().vehicleType())
                .build();

        driverRepository.save(driver);
        vehicleRepository.save(vehicle);
    }
}