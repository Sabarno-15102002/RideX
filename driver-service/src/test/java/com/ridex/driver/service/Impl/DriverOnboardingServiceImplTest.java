package com.ridex.driver.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import com.ridex.driver.dto.request.DriverOnboardingRequest;
import com.ridex.driver.dto.request.VehicleOnboardingRequest;
import com.ridex.driver.entity.Driver;
import com.ridex.driver.exception.ResourceAlreadyExistsException;
import com.ridex.driver.exception.ResourceNotFoundException;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.VehicleRepository;
import com.ridex.driver.utilities.DriverStatus;
import com.ridex.driver.utilities.VehicleType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverOnboardingServiceImplTest {

        @Mock
        private DriverRepository driverRepository;

        @Mock
        private VehicleRepository vehicleRepository;

        @InjectMocks
        private DriverOnboardingServiceImpl onboardingService;

        private UUID driverId;
        private Driver driver;
        private DriverOnboardingRequest request;

        @BeforeEach
        void setUp() {
                driverId = UUID.randomUUID();

                driver = Driver.builder()
                                .id(driverId)
                                .userId(UUID.randomUUID())
                                .name("John")
                                .status(DriverStatus.OFFLINE)
                                .build();

                request = new DriverOnboardingRequest(
                                "DL123456",
                                new VehicleOnboardingRequest(
                                                "KA01AB1234",
                                                "Toyota",
                                                "Camry",
                                                "White",
                                                VehicleType.SEDAN));
        }

        @Test
        void onboard_shouldSaveDriverAndVehicle() {
                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                when(vehicleRepository.existsByRegistrationNumber("KA01AB1234"))
                                .thenReturn(false);

                onboardingService.onboard(driverId, request);

                assertEquals("DL123456", driver.getLicenseNumber());

                verify(driverRepository).save(driver);

                verify(vehicleRepository).save(argThat(vehicle -> vehicle.getDriverId().equals(driverId)
                                && vehicle.getRegistrationNumber().equals("KA01AB1234")
                                && vehicle.getMake().equals("Toyota")
                                && vehicle.getModel().equals("Camry")
                                && vehicle.getColor().equals("White")
                                && vehicle.getVehicleType() == VehicleType.SEDAN));
        }

        @Test
        void onboard_shouldThrowWhenDriverDoesNotExist() {
                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> onboardingService.onboard(driverId, request));

                verifyNoInteractions(vehicleRepository);
        }

        @Test
        void onboard_shouldRejectAlreadyOnboardedDriver() {
                driver.setLicenseNumber("EXISTING-LICENSE");

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                assertThrows(
                                ResourceAlreadyExistsException.class,
                                () -> onboardingService.onboard(driverId, request));

                verifyNoInteractions(vehicleRepository);
                verify(driverRepository, never()).save(any());
        }

        @Test
        void onboard_shouldRejectDuplicateVehicleRegistration() {
                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                when(vehicleRepository.existsByRegistrationNumber("KA01AB1234"))
                                .thenReturn(true);

                assertThrows(
                                ResourceAlreadyExistsException.class,
                                () -> onboardingService.onboard(driverId, request));

                assertNull(driver.getLicenseNumber());

                verify(driverRepository, never()).save(any());
                verify(vehicleRepository, never()).save(any());
        }
}
