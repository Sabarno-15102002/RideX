package com.ridex.rider.service.Impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.rider.dto.request.CreateSavedLocationRequest;
import com.ridex.rider.dto.response.SavedLocationResponse;
import com.ridex.rider.entity.Rider;
import com.ridex.rider.entity.SavedLocation;
import com.ridex.rider.exception.RiderNotFoundException;
import com.ridex.rider.exception.SavedLocationNotFoundException;
import com.ridex.rider.repository.RiderRepository;
import com.ridex.rider.repository.SavedLocationRepository;
import com.ridex.rider.service.SavedLocationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SavedLocationServiceImpl implements SavedLocationService {

    private final RiderRepository riderRepository;
    private final SavedLocationRepository savedLocationRepository;

    @Override
    @Transactional
    public SavedLocationResponse create(
            UUID userId,
            CreateSavedLocationRequest request
    ) {

        Rider rider = riderRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RiderNotFoundException(
                                "Rider profile not found"
                        ));

        SavedLocation location =
                SavedLocation.builder()
                        .riderId(rider.getId())
                        .label(request.label())
                        .latitude(request.latitude())
                        .longitude(request.longitude())
                        .address(request.address())
                        .build();

        location =
                savedLocationRepository.save(location);

        return toResponse(location);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedLocationResponse> getAll(
            UUID userId
    ) {

        Rider rider = riderRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RiderNotFoundException(
                                "Rider profile not found"
                        ));

        return savedLocationRepository
                .findAllByRiderId(rider.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(
            UUID userId,
            UUID locationId
    ) {

        Rider rider = riderRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RiderNotFoundException(
                                "Rider profile not found"
                        ));

        SavedLocation location =
                savedLocationRepository
                        .findByIdAndRiderId(
                                locationId,
                                rider.getId()
                        )
                        .orElseThrow(() ->
                                new SavedLocationNotFoundException(
                                        "Saved location not found"
                                ));

        savedLocationRepository.delete(location);
    }

    private SavedLocationResponse toResponse(
            SavedLocation location
    ) {

        return new SavedLocationResponse(
                location.getId(),
                location.getLabel(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAddress(),
                location.getCreatedAt()
        );
    }
}