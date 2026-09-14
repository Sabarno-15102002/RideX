package com.ridex.driver.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridex.driver.dto.request.ReserveDriverRequest;
import com.ridex.driver.dto.response.DriverReservationResponse;
import com.ridex.driver.service.Impl.DriverReservationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/drivers")
@RequiredArgsConstructor
public class DriverReservationController {

    private final DriverReservationService reservationService;

    @PostMapping("/{driverId}/reservations")
    public ResponseEntity<DriverReservationResponse> reserveDriver(
            @PathVariable UUID driverId,
            @Valid @RequestBody ReserveDriverRequest request
    ) {

        boolean reserved =
                reservationService.reserveDriver(
                        driverId,
                        request.tripId()
                );

        return ResponseEntity.ok(
                new DriverReservationResponse(reserved)
        );
    }
}