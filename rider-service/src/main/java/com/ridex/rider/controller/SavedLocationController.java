package com.ridex.rider.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridex.rider.config.SecurityUtils;
import com.ridex.rider.dto.request.CreateSavedLocationRequest;
import com.ridex.rider.dto.response.SavedLocationResponse;
import com.ridex.rider.service.SavedLocationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/riders/me/locations")
@RequiredArgsConstructor
public class SavedLocationController {

    private final SavedLocationService savedLocationService;

    @PostMapping
    public ResponseEntity<SavedLocationResponse> create(
            @Valid @RequestBody CreateSavedLocationRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        savedLocationService.create(
                                SecurityUtils.getCurrentUserId(),
                                request
                        )
                );
    }

    @GetMapping
    public ResponseEntity<List<SavedLocationResponse>> getAll() {

        return ResponseEntity.ok(
                savedLocationService.getAll(
                        SecurityUtils.getCurrentUserId()
                )
        );
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID locationId
    ) {

        savedLocationService.delete(
                SecurityUtils.getCurrentUserId(),
                locationId
        );

        return ResponseEntity.noContent().build();
    }
}
