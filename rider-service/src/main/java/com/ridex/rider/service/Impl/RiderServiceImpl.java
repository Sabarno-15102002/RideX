package com.ridex.rider.service.Impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.rider.dto.response.RiderResponse;
import com.ridex.rider.dto.response.UpdateRiderRequest;
import com.ridex.rider.entity.Rider;
import com.ridex.rider.exception.RiderNotFoundException;
import com.ridex.rider.repository.RiderRepository;
import com.ridex.rider.service.RiderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiderServiceImpl implements RiderService {

    private final RiderRepository riderRepository;

    @Override
    @Transactional(readOnly = true)
    public RiderResponse getMyProfile(UUID userId) {

        Rider rider = riderRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RiderNotFoundException(
                                "Rider profile not found"
                        ));

        return toResponse(rider);
    }

    @Override
    @Transactional
    public RiderResponse updateMyProfile(
            UUID userId,
            UpdateRiderRequest request
    ) {

        Rider rider = riderRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RiderNotFoundException(
                                "Rider profile not found"
                        ));

        rider.setName(request.name());

        return toResponse(riderRepository.save(rider));
    }

    private RiderResponse toResponse(Rider rider) {

        return new RiderResponse(
                rider.getId(),
                rider.getUserId(),
                rider.getName(),
                rider.getRating()
        );
    }
}