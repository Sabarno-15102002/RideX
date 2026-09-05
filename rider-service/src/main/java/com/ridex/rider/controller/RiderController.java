package com.ridex.rider.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridex.rider.config.SecurityUtils;
import com.ridex.rider.dto.response.RiderResponse;
import com.ridex.rider.dto.response.UpdateRiderRequest;
import com.ridex.rider.service.RiderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/riders")
@RequiredArgsConstructor
public class RiderController {

    private final RiderService riderService;

    @GetMapping("/me")
    public ResponseEntity<RiderResponse> getMyProfile() {

        return ResponseEntity.ok(
                riderService.getMyProfile(
                        SecurityUtils.getCurrentUserId()
                )
        );
    }

    @PutMapping("/me")
    public ResponseEntity<RiderResponse> updateMyProfile(
            @Valid @RequestBody UpdateRiderRequest request
    ) {

        return ResponseEntity.ok(
                riderService.updateMyProfile(
                        SecurityUtils.getCurrentUserId(),
                        request
                )
        );
    }
}