package com.ridex.driver.controller;

import com.ridex.driver.dto.AuthenticatedUser;
import com.ridex.driver.dto.request.*;
import com.ridex.driver.dto.response.*;
import com.ridex.driver.service.*;
import com.ridex.driver.utilities.DriverStatus;
import com.ridex.driver.utilities.VehicleType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class DriverControllerTest {

    @org.mockito.Mock
    private DriverOnboardingService driverOnboardingService;

    @org.mockito.Mock
    private DriverService driverService;

    @org.mockito.Mock
    private DriverTripService driverTripService;

    private DriverController controller;

    private UUID driverId;
    private UUID tripId;

    @BeforeEach
    void setUp() {
        controller = new DriverController(
                driverOnboardingService,
                driverService,
                driverTripService);

        driverId = UUID.randomUUID();
        tripId = UUID.randomUUID();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(driverId, "DRIVER"),
                        null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onboardDriver_shouldReturnNoContent() {
        DriverOnboardingRequest request = new DriverOnboardingRequest("LICENSE-123", null);

        ResponseEntity<Void> response = controller.onboardDriver(driverId, request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(driverOnboardingService).onboard(driverId, request);
    }

    @Test
    void getDriver_shouldReturnCurrentDriver() {
        DriverResponse expected = new DriverResponse(
                UUID.randomUUID(),
                driverId,
                "Alex",
                "LICENSE-123",
                BigDecimal.valueOf(4.8),
                DriverStatus.AVAILABLE);

        when(driverService.getDriver(driverId)).thenReturn(expected);

        ResponseEntity<DriverResponse> response = controller.getDriver();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(driverService).getDriver(driverId);
    }

    @Test
    void updateStatus_shouldDelegateToService() {
        UpdateDriverStatusRequest request = new UpdateDriverStatusRequest(DriverStatus.AVAILABLE);

        DriverResponse expected = new DriverResponse(
                UUID.randomUUID(), driverId, "Alex",
                "LICENSE-123", BigDecimal.valueOf(4.8),
                DriverStatus.AVAILABLE);

        when(driverService.updateStatus(driverId, request.status()))
                .thenReturn(expected);

        ResponseEntity<DriverResponse> response = controller.updateStatus(driverId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(driverService).updateStatus(driverId, request.status());
    }

    @Test
    void updateMyProfile_shouldUseAuthenticatedUserId() {
        UpdateDriverRequest request = new UpdateDriverRequest("Alex Updated");

        DriverResponse expected = new DriverResponse(
                UUID.randomUUID(), driverId, "Alex Updated",
                "LICENSE-123", BigDecimal.valueOf(4.8),
                DriverStatus.AVAILABLE);

        when(driverService.updateMyProfile(driverId, request))
                .thenReturn(expected);

        ResponseEntity<DriverResponse> response = controller.updateMyProfile(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(driverService).updateMyProfile(driverId, request);
    }

    @Test
    void getVehicle_shouldReturnVehicle() {
        VehicleResponse expected = new VehicleResponse(
                UUID.randomUUID(), driverId, "KA01AB1234",
                "Toyota", "Etios", "White", VehicleType.SUV);

        when(driverService.getVehicle(driverId)).thenReturn(expected);

        ResponseEntity<VehicleResponse> response = controller.getVehicle(driverId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(driverService).getVehicle(driverId);
    }

    @Test
    void updateVehicle_shouldDelegateToService() {
        UpdateVehicleRequest request = new UpdateVehicleRequest(
                "KA01AB1234", "Toyota", "Etios", "White",
                VehicleType.SUV);

        VehicleResponse expected = new VehicleResponse(
                UUID.randomUUID(), driverId, "KA01AB1234",
                "Toyota", "Etios", "White", VehicleType.SUV);

        when(driverService.updateVehicle(driverId, request))
                .thenReturn(expected);

        ResponseEntity<VehicleResponse> response = controller.updateVehicle(driverId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(driverService).updateVehicle(driverId, request);
    }

    @Test
    void acceptTrip_shouldUseAuthenticatedUserId() {
        ResponseEntity<Void> response = controller.acceptTrip(tripId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(driverTripService).acceptTrip(driverId, tripId);
    }

    @Test
    void rejectTrip_shouldUseAuthenticatedUserId() {
        ResponseEntity<Void> response = controller.rejectTrip(tripId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(driverTripService).rejectTrip(driverId, tripId);
    }

    @Test
    void arriveAtPickup_shouldUseAuthenticatedUserId() {
        ResponseEntity<Void> response = controller.arriveAtPickup(tripId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(driverTripService).arriveAtPickup(driverId, tripId);
    }

    @Test
    void startTrip_shouldUseAuthenticatedUserId() {
        ResponseEntity<Void> response = controller.startTrip(tripId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(driverTripService).startTrip(driverId, tripId);
    }

    @Test
    void completeTrip_shouldUseAuthenticatedUserId() {
        ResponseEntity<Void> response = controller.completeTrip(tripId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(driverTripService).completeTrip(driverId, tripId);
    }
}
