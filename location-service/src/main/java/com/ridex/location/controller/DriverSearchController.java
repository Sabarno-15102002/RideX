package com.ridex.location.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ridex.location.dto.response.NearbyDriverResponse;
import com.ridex.location.service.DriverLocationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/drivers")
@RequiredArgsConstructor
public class DriverSearchController {

    private final DriverLocationService driverLocationService;
    
    @GetMapping("/nearby")
        public ResponseEntity<List<NearbyDriverResponse>> findNearbyDrivers(
                        @RequestParam double latitude,
                        @RequestParam double longitude,
                        @RequestParam double radiusKm) {

                return ResponseEntity.ok(
                                driverLocationService.findNearbyDrivers(
                                                latitude,
                                                longitude,
                                                radiusKm));
        }
}
