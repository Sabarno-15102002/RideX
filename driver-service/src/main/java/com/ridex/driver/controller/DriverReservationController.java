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
import com.ridex.driver.service.DriverReservationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/internal/drivers")
@RequiredArgsConstructor
@Slf4j 
public class DriverReservationController {

        private final DriverReservationService reservationService;

        @PostMapping("/{driverId}/reservations")
        public ResponseEntity<DriverReservationResponse> reserveDriver(
                        @PathVariable UUID driverId,
                        @Valid @RequestBody ReserveDriverRequest request) {

                log.info(
                        "Internal driver reservation request received: driverId={}, tripId={}",
                        driverId,
                        request.tripId()
                );
                boolean reserved = reservationService.reserveDriver(
                                driverId,
                                request.tripId());

                return ResponseEntity.ok(
                                new DriverReservationResponse(reserved));
        }

        @PostMapping("/{driverId}/reservations/{tripId}/accept")
        public ResponseEntity<Void> acceptReservation(
                        @PathVariable UUID driverId,
                        @PathVariable UUID tripId) {
                reservationService.acceptReservation(driverId, tripId);
                return ResponseEntity.noContent().build();
        }

        @PostMapping("/{driverId}/reservations/{tripId}/release")
        public ResponseEntity<Void> releaseReservation(
                @PathVariable UUID driverId,
                @PathVariable UUID tripId
        ) {
                reservationService.releaseReservation(driverId, tripId);
                return ResponseEntity.noContent().build();
        }
}