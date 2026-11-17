package com.ridex.driver.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.ridex.driver.dto.request.UpdateDriverRequest;
import com.ridex.driver.dto.request.UpdateVehicleRequest;
import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.Vehicle;
import com.ridex.driver.event.DriverStatusChangedEvent;
import com.ridex.driver.exception.ResourceNotFoundException;
import com.ridex.driver.exception.TripStateException;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.VehicleRepository;
import com.ridex.driver.service.utilities.OutboxEventService;
import com.ridex.driver.utilities.DriverStatus;
import com.ridex.driver.utilities.VehicleType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

        @Mock
        private DriverRepository driverRepository;

        @Mock
        private VehicleRepository vehicleRepository;

        @Mock
        private OutboxEventService outboxEventService;

        @InjectMocks
        private DriverServiceImpl driverService;

        private UUID driverId;
        private UUID userId;
        private Driver driver;

        @BeforeEach
        void setUp() {
                driverId = UUID.randomUUID();
                userId = UUID.randomUUID();

                driver = Driver.builder()
                                .id(driverId)
                                .userId(userId)
                                .name("John")
                                .licenseNumber("DL12345")
                                .rating(BigDecimal.valueOf(5.00))
                                .status(DriverStatus.OFFLINE)
                                .build();
        }

        @Test
        void getDriver_shouldReturnDriverResponse() {
                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                var response = driverService.getDriver(userId);

                assertEquals(driverId, response.id());
                assertEquals(userId, response.userId());
                assertEquals("John", response.name());
                assertEquals("DL12345", response.licenseNumber());
                assertEquals(DriverStatus.OFFLINE, response.status());
        }

        @Test
        void getDriver_shouldThrowWhenDriverDoesNotExist() {
                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> driverService.getDriver(userId));
        }

        @Test
        void updateStatus_shouldChangeOfflineToAvailableAndPublishEvent() {
                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                var response = driverService.updateStatus(
                                driverId,
                                DriverStatus.AVAILABLE);

                assertEquals(DriverStatus.AVAILABLE, response.status());

                verify(outboxEventService)
                                .saveDriverStatusChangedEvent(
                                                any(DriverStatusChangedEvent.class));
        }

        @Test
        void updateStatus_shouldChangeAvailableToOffline() {
                driver.setStatus(DriverStatus.AVAILABLE);

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                var response = driverService.updateStatus(
                                driverId,
                                DriverStatus.OFFLINE);

                assertEquals(DriverStatus.OFFLINE, response.status());

                verify(outboxEventService)
                                .saveDriverStatusChangedEvent(any());
        }

        @Test
        void updateStatus_shouldRejectOfflineToOnTrip() {
                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                assertThrows(
                                TripStateException.class,
                                () -> driverService.updateStatus(
                                                driverId,
                                                DriverStatus.ON_TRIP));

                verifyNoInteractions(outboxEventService);
        }

        @Test
        void updateStatus_shouldRejectAvailableToOnTrip() {
                driver.setStatus(DriverStatus.AVAILABLE);

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                assertThrows(
                                TripStateException.class,
                                () -> driverService.updateStatus(
                                                driverId,
                                                DriverStatus.ON_TRIP));
        }

        @Test
        void updateStatus_shouldRejectOnTripToOffline() {
                driver.setStatus(DriverStatus.ON_TRIP);

                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.of(driver));

                assertThrows(
                                TripStateException.class,
                                () -> driverService.updateStatus(
                                                driverId,
                                                DriverStatus.OFFLINE));
        }

        @Test
        void updateStatus_shouldThrowWhenDriverDoesNotExist() {
                when(driverRepository.findById(driverId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> driverService.updateStatus(
                                                driverId,
                                                DriverStatus.AVAILABLE));
        }

        @Test
        void getVehicle_shouldReturnVehicleResponse() {
                Vehicle vehicle = createVehicle();

                when(driverRepository.existsById(driverId))
                                .thenReturn(true);

                when(vehicleRepository.findFirstByDriverId(driverId))
                                .thenReturn(Optional.of(vehicle));

                var response = driverService.getVehicle(driverId);

                assertEquals(vehicle.getId(), response.id());
                assertEquals(driverId, response.driverId());
                assertEquals("KA01AB1234", response.registrationNumber());
                assertEquals(VehicleType.SEDAN, response.vehicleType());
        }

        @Test
        void getVehicle_shouldThrowWhenDriverDoesNotExist() {
                when(driverRepository.existsById(driverId))
                                .thenReturn(false);

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> driverService.getVehicle(driverId));

                verifyNoInteractions(vehicleRepository);
        }

        @Test
        void getVehicle_shouldThrowWhenVehicleDoesNotExist() {
                when(driverRepository.existsById(driverId))
                                .thenReturn(true);

                when(vehicleRepository.findFirstByDriverId(driverId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> driverService.getVehicle(driverId));
        }

        @Test
        void updateMyProfile_shouldUpdateDriverName() {
                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.of(driver));

                when(driverRepository.save(any(Driver.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                var response = driverService.updateMyProfile(
                                userId,
                                new UpdateDriverRequest("John Updated"));

                assertEquals("John Updated", response.name());

                verify(driverRepository).save(driver);
        }

        @Test
        void updateMyProfile_shouldThrowWhenDriverDoesNotExist() {
                when(driverRepository.findByUserId(userId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> driverService.updateMyProfile(
                                                userId,
                                                new UpdateDriverRequest("John Updated")));

                verify(driverRepository, never()).save(any());
        }

        @Test
        void updateVehicle_shouldUpdateAllProvidedFields() {
                Vehicle vehicle = createVehicle();

                when(driverRepository.existsById(driverId))
                                .thenReturn(true);

                when(vehicleRepository.findFirstByDriverId(driverId))
                                .thenReturn(Optional.of(vehicle));

                when(vehicleRepository.save(any(Vehicle.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                var response = driverService.updateVehicle(
                                driverId,
                                new UpdateVehicleRequest(
                                                "KA02CD5678",
                                                "Honda",
                                                "City",
                                                "Black",
                                                VehicleType.SEDAN));

                assertEquals("KA02CD5678", response.registrationNumber());
                assertEquals("Honda", response.make());
                assertEquals("City", response.model());
                assertEquals("Black", response.color());

                verify(vehicleRepository).save(vehicle);
        }

        @Test
        void updateVehicle_shouldUpdateOnlyNonNullFields() {
                Vehicle vehicle = createVehicle();

                when(driverRepository.existsById(driverId))
                                .thenReturn(true);

                when(vehicleRepository.findFirstByDriverId(driverId))
                                .thenReturn(Optional.of(vehicle));

                when(vehicleRepository.save(any(Vehicle.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                var response = driverService.updateVehicle(
                                driverId,
                                new UpdateVehicleRequest(
                                                null,
                                                "Honda",
                                                "City",
                                                "Black",
                                                null));

                assertEquals("KA01AB1234", response.registrationNumber());
                assertEquals("Honda", response.make());
                assertEquals("City", response.model());
                assertEquals("Black", response.color());
                assertEquals(VehicleType.SEDAN, response.vehicleType());
        }

        @Test
        void updateVehicle_shouldThrowWhenDriverDoesNotExist() {
                when(driverRepository.existsById(driverId))
                                .thenReturn(false);

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> driverService.updateVehicle(
                                                driverId,
                                                new UpdateVehicleRequest(
                                                                null, null, null, null, null)));

                verifyNoInteractions(vehicleRepository);
        }

        @Test
        void updateVehicle_shouldThrowWhenVehicleDoesNotExist() {
                when(driverRepository.existsById(driverId))
                                .thenReturn(true);

                when(vehicleRepository.findFirstByDriverId(driverId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> driverService.updateVehicle(
                                                driverId,
                                                new UpdateVehicleRequest(
                                                                null, null, null, null, null)));
        }

        private Vehicle createVehicle() {
                return Vehicle.builder()
                                .id(UUID.randomUUID())
                                .driverId(driverId)
                                .registrationNumber("KA01AB1234")
                                .make("Toyota")
                                .model("Camry")
                                .color("White")
                                .vehicleType(VehicleType.SEDAN)
                                .build();
        }
}
