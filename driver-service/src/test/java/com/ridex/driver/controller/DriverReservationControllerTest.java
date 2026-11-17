package com.ridex.driver.controller;

import com.ridex.driver.dto.request.ReserveDriverRequest;
import com.ridex.driver.dto.response.DriverReservationResponse;
import com.ridex.driver.service.DriverReservationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverReservationControllerTest {

    @Mock
    private DriverReservationService reservationService;

    private DriverReservationController controller;

    private UUID driverId;
    private UUID tripId;

    @BeforeEach
    void setUp() {
        controller = new DriverReservationController(reservationService);
        driverId = UUID.randomUUID();
        tripId = UUID.randomUUID();
    }

    @Test
    void reserveDriver_whenReserved_shouldReturnTrue() {
        ReserveDriverRequest request = new ReserveDriverRequest(tripId);

        when(reservationService.reserveDriver(driverId, tripId))
                .thenReturn(true);

        ResponseEntity<DriverReservationResponse> response = controller.reserveDriver(driverId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().reserved());

        verify(reservationService).reserveDriver(driverId, tripId);
    }

    @Test
    void reserveDriver_whenNotReserved_shouldReturnFalse() {
        ReserveDriverRequest request = new ReserveDriverRequest(tripId);

        when(reservationService.reserveDriver(driverId, tripId))
                .thenReturn(false);

        ResponseEntity<DriverReservationResponse> response = controller.reserveDriver(driverId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().reserved());
    }

    @Test
    void acceptReservation_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.acceptReservation(driverId, tripId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reservationService).acceptReservation(driverId, tripId);
    }

    @Test
    void releaseReservation_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.releaseReservation(driverId, tripId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reservationService).releaseReservation(driverId, tripId);
    }
}
