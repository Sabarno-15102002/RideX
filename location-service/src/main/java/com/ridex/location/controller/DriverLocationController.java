package com.ridex.location.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridex.location.config.SecurityUtils;
import com.ridex.location.dto.request.UpdateLocationRequest;
import com.ridex.location.service.DriverLocationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverLocationController {

        private final DriverLocationService driverLocationService;

        @PutMapping("/me/location")
        public ResponseEntity<Void> updateMyLocation(
                        @Valid @RequestBody UpdateLocationRequest request) {
                UUID userId = SecurityUtils.getCurrentUserId();

                driverLocationService.updateDriverLocation(
                                userId,
                                request.latitude(),
                                request.longitude());

                return ResponseEntity.noContent().build();
        }
}