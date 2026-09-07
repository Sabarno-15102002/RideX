package com.ridex.driver.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridex.driver.config.SecurityUtils;
import com.ridex.driver.dto.request.DriverOnboardingRequest;
import com.ridex.driver.dto.request.UpdateDriverStatusRequest;
import com.ridex.driver.dto.response.DriverResponse;
import com.ridex.driver.dto.response.VehicleResponse;
import com.ridex.driver.service.DriverOnboardingService;
import com.ridex.driver.service.DriverService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

        private final DriverOnboardingService driverOnboardingService;
        private final DriverService driverService;

        @PutMapping("/{driverId}/onboarding")
        public ResponseEntity<Void> onboardDriver(
                        @PathVariable UUID driverId,
                        @Valid @RequestBody DriverOnboardingRequest request) {

                driverOnboardingService.onboard(driverId, request);

                return ResponseEntity.noContent().build();
        }

        @GetMapping("/")
        public ResponseEntity<DriverResponse> getDriver() {
                UUID userId = SecurityUtils.getCurrentUserId();
                return ResponseEntity.ok(
                                driverService.getDriver(userId));
        }

        @PatchMapping("/{driverId}/status")
        public ResponseEntity<DriverResponse> updateStatus(
                        @PathVariable UUID driverId,
                        @Valid @RequestBody UpdateDriverStatusRequest request) {

                return ResponseEntity.ok(
                                driverService.updateStatus(
                                                driverId,
                                                request.status()));
        }

        @GetMapping("/{driverId}/vehicle")
        public ResponseEntity<VehicleResponse> getVehicle(
                        @PathVariable UUID driverId) {

                return ResponseEntity.ok(
                                driverService.getVehicle(driverId));
        }
}