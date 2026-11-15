package com.ridex.trip.controller;

import com.ridex.trip.config.SecurityUtils;
import com.ridex.trip.dto.AuthenticatedUser;
import com.ridex.trip.dto.PaymentStatus;
import com.ridex.trip.dto.TripStatus;
import com.ridex.trip.dto.request.CreateTripRequest;
import com.ridex.trip.dto.response.TripPaymentResponse;
import com.ridex.trip.dto.response.TripResponse;
import com.ridex.trip.dto.response.TripStatusResponse;
import com.ridex.trip.exception.InvalidCredentialsException;
import com.ridex.trip.service.RiderIdentityService;
import com.ridex.trip.service.TripService;
import com.ridex.trip.service.util.TripPaymentQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripControllerTest {
    @Mock TripService tripService;
    @Mock TripPaymentQueryService paymentQueryService;
    @Mock RiderIdentityService riderIdentityService;
    TripController controller;

    UUID userId, riderId, tripId;

    @BeforeEach void setUp() {
        controller = new TripController(tripService, paymentQueryService, riderIdentityService);
        userId = UUID.randomUUID(); riderId = UUID.randomUUID(); tripId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(userId, "RIDER"), null, List.of()));
    }

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test void createTrip_shouldUseAuthenticatedUserAndReturnOk() {
        CreateTripRequest request = new CreateTripRequest(12.9, 77.5, 12.8, 77.6);
        TripResponse expected = new TripResponse(tripId, riderId, null, 12.9, 77.5,
                12.8, 77.6, TripStatus.REQUESTED, Instant.now(), null);
        when(tripService.createTrip(userId, request)).thenReturn(expected);

        ResponseEntity<TripResponse> response = controller.createTrip(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(tripService).createTrip(userId, request);
    }

    @Test void createTrip_shouldFailWhenNoAuthenticatedUserExists() {
        SecurityContextHolder.clearContext();
        CreateTripRequest request = new CreateTripRequest(12.9, 77.5, 12.8, 77.6);

        assertThrows(InvalidCredentialsException.class, () -> controller.createTrip(request));
        verifyNoInteractions(tripService);
    }

    @Test void getPayment_shouldResolveRiderIdBeforeQueryingPayment() {
        TripPaymentResponse expected = new TripPaymentResponse(tripId, UUID.randomUUID(),
                new BigDecimal("250.00"), "INR", PaymentStatus.PENDING, null, null);
        when(riderIdentityService.getRiderIdByUserId(userId)).thenReturn(riderId);
        when(paymentQueryService.getPaymentForTrip(tripId, riderId)).thenReturn(expected);

        ResponseEntity<TripPaymentResponse> response = controller.getPayment(tripId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(riderIdentityService).getRiderIdByUserId(userId);
        verify(paymentQueryService).getPaymentForTrip(tripId, riderId);
    }

    @Test void getTripStatus_shouldReturnTripStatus() {
        TripStatusResponse expected = new TripStatusResponse(TripStatus.TRIP_STARTED);
        when(tripService.getTripStatus(tripId)).thenReturn(expected);

        ResponseEntity<TripStatusResponse> response = controller.getTripStatus(tripId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(tripService).getTripStatus(tripId);
    }

    @Test void getTrip_shouldUseAuthenticatedUserForOwnershipAwareLookup() {
        TripResponse expected = new TripResponse(tripId, riderId, null, 12.9, 77.5,
                12.8, 77.6, TripStatus.REQUESTED, Instant.now(), null);
        when(tripService.getTrip(userId, tripId)).thenReturn(expected);

        ResponseEntity<TripResponse> response = controller.getTrip(tripId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(tripService).getTrip(userId, tripId);
    }
}
