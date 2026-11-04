package com.ridex.rider.service;

import java.util.UUID;

import com.ridex.rider.dto.request.UpdateRiderRequest;
import com.ridex.rider.dto.response.RiderResponse;

public interface RiderService {

    RiderResponse updateMyProfile(UUID userId, UpdateRiderRequest request);

    RiderResponse getMyProfile(UUID userId);

}