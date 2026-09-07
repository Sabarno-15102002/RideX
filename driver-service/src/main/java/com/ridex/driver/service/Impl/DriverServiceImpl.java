package com.ridex.driver.service.Impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.driver.dto.response.DriverResponse;
import com.ridex.driver.dto.response.VehicleResponse;
import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.Vehicle;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.VehicleRepository;
import com.ridex.driver.service.DriverService;
import com.ridex.driver.utilities.DriverStatus;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

        private final DriverRepository driverRepository;
        private final VehicleRepository vehicleRepository;

        @Override
        public DriverResponse getDriver(UUID userId) {

                Driver driver = driverRepository.findByUserId(userId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Driver not found: " + userId));

                return new DriverResponse(
                                driver.getId(),
                                driver.getUserId(),
                                driver.getName(),
                                driver.getLicenseNumber(),
                                driver.getRating(),
                                driver.getStatus());
        }

        @Override
        @Transactional
        public DriverResponse updateStatus(
                        UUID driverId,
                        DriverStatus requestedStatus) {

                Driver driver = driverRepository.findById(driverId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Driver not found: " + driverId));

                DriverStatus currentStatus = driver.getStatus();

                if (!isValidTransition(currentStatus, requestedStatus)) {
                        throw new IllegalStateException(
                                        "Invalid driver status transition: "
                                                        + currentStatus
                                                        + " -> "
                                                        + requestedStatus);
                }

                driver.setStatus(requestedStatus);

                return new DriverResponse(
                                driver.getId(),
                                driver.getUserId(),
                                driver.getName(),
                                driver.getLicenseNumber(),
                                driver.getRating(),
                                driver.getStatus());
        }

        private boolean isValidTransition(
                        DriverStatus current,
                        DriverStatus requested) {

                return switch (current) {

                        case OFFLINE ->
                                requested == DriverStatus.AVAILABLE;

                        case AVAILABLE ->
                                requested == DriverStatus.OFFLINE;

                        case ON_TRIP ->
                                false;
                };
        }

        @Override 
        public VehicleResponse getVehicle(UUID driverId) {

                // First verify that the driver exists
                if (!driverRepository.existsById(driverId)) {
                        throw new EntityNotFoundException(
                                        "Driver not found: " + driverId);
                }

                Vehicle vehicle = vehicleRepository.findFirstByDriverId(driverId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Vehicle not found for driver: " + driverId));

                return new VehicleResponse(
                                vehicle.getId(),
                                vehicle.getDriverId(),
                                vehicle.getRegistrationNumber(),
                                vehicle.getMake(),
                                vehicle.getModel(),
                                vehicle.getColor(),
                                vehicle.getVehicleType());
        }
}