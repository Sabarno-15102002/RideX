package com.ridex.trip.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridex.trip.config.SecurityUtils;
import com.ridex.trip.dto.request.CreateTripRequest;
import com.ridex.trip.dto.response.TripPaymentResponse;
import com.ridex.trip.dto.response.TripResponse;
import com.ridex.trip.service.RiderIdentityService;
import com.ridex.trip.service.TripService;
import com.ridex.trip.service.util.TripPaymentQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final TripPaymentQueryService queryService;
    private final RiderIdentityService riderIdentityService;

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @Valid @RequestBody CreateTripRequest request
    ) {

        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                tripService.createTrip(userId, request)
        );
    }

    @GetMapping("/{tripId}/payment")
    public TripPaymentResponse getPayment(@PathVariable UUID tripId) {

        UUID userId = SecurityUtils.getCurrentUserId();

        return queryService.getPaymentForTrip(
                tripId,
                riderIdentityService.getRiderIdByUserId(userId)
        );
    }
}