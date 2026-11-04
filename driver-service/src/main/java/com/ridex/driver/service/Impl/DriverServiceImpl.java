package com.ridex.driver.service.Impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.driver.dto.request.UpdateDriverRequest;
import com.ridex.driver.dto.request.UpdateVehicleRequest;
import com.ridex.driver.dto.response.DriverResponse;
import com.ridex.driver.dto.response.VehicleResponse;
import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.Vehicle;
import com.ridex.driver.event.DriverStatusChangedEvent;
import com.ridex.driver.exception.ResourceNotFoundException;
import com.ridex.driver.exception.TripStateException;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.VehicleRepository;
import com.ridex.driver.service.DriverService;
import com.ridex.driver.service.utilities.OutboxEventService;
import com.ridex.driver.utilities.DriverStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

        private final DriverRepository driverRepository;
        private final VehicleRepository vehicleRepository;

        private final OutboxEventService outboxEventService;

        @Override
        @Cacheable(
                cacheNames = "driverProfile",
                key = "#userId",
                unless = "#result == null"
        )
        public DriverResponse getDriver(UUID userId) {

                Driver driver = driverRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
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
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Driver not found: " + driverId));

                DriverStatus currentStatus = driver.getStatus();

                if (!isValidTransition(currentStatus, requestedStatus)) {
                        throw new TripStateException(
                                        "Invalid driver status transition: "
                                                        + currentStatus
                                                        + " -> "
                                                        + requestedStatus);
                }

                driver.setStatus(requestedStatus);
                DriverStatusChangedEvent event = new DriverStatusChangedEvent(
                                UUID.randomUUID(),
                                driver.getId(),
                                driver.getUserId(),
                                driver.getStatus().name(),
                                Instant.now());

                outboxEventService.saveDriverStatusChangedEvent(event);

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
        @Cacheable(
                cacheNames = "vehicleDetails",
                key = "#driverId",
                unless = "#result == null"
        )
        public VehicleResponse getVehicle(UUID driverId) {

                // First verify that the driver exists
                if (!driverRepository.existsById(driverId)) {
                        throw new ResourceNotFoundException(
                                        "Driver not found: " + driverId);
                }

                Vehicle vehicle = vehicleRepository.findFirstByDriverId(driverId)
                                .orElseThrow(() -> new ResourceNotFoundException(
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

        @Override
        @CacheEvict(
                cacheNames = "driverProfile",
                key = "#userId"
        )
        public DriverResponse updateMyProfile(UUID userId, UpdateDriverRequest request) {
                Driver driver = driverRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Driver not found: " + userId));

                // Update the driver's profile with the new information
                driver.setName(request.name());

                // Save the updated driver information
                driverRepository.save(driver);

                return new DriverResponse(
                                driver.getId(),
                                driver.getUserId(),
                                driver.getName(),
                                driver.getLicenseNumber(),
                                driver.getRating(),
                                driver.getStatus());
        }

        @Override
        @CacheEvict(
                cacheNames = "vehicleDetails",
                key = "#driverId"
        )
        public VehicleResponse updateVehicle(UUID driverId, UpdateVehicleRequest request) {
                if (!driverRepository.existsById(driverId)) {
                        throw new ResourceNotFoundException(
                                        "Driver not found: " + driverId);
                }

                Vehicle vehicle = vehicleRepository.findFirstByDriverId(driverId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Vehicle not found for driver: " + driverId));

                // Update the vehicle information with the new details
                if (request.registrationNumber() != null) {
                        vehicle.setRegistrationNumber(request.registrationNumber());
                }

                if (request.make() != null) {
                        vehicle.setMake(request.make());
                }

                if (request.model() != null) {
                        vehicle.setModel(request.model());
                }

                if (request.color() != null) {
                        vehicle.setColor(request.color());
                }

                if (request.vehicleType() != null) {
                        vehicle.setVehicleType(request.vehicleType());
                }

                // Save the updated vehicle information
                vehicleRepository.save(vehicle);

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