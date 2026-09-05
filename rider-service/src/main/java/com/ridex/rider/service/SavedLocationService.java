package com.ridex.rider.service;

import java.util.List;
import java.util.UUID;

import com.ridex.rider.dto.request.CreateSavedLocationRequest;
import com.ridex.rider.dto.response.SavedLocationResponse;

public interface SavedLocationService {

    SavedLocationResponse create(
            UUID userId,
            CreateSavedLocationRequest request
    );

    List<SavedLocationResponse> getAll(
            UUID userId
    );

    void delete(
            UUID userId,
            UUID locationId
    );

}