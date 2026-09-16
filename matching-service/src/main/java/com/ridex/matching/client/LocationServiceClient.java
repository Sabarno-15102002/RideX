package com.ridex.matching.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ridex.matching.dto.response.NearbyDriverResponse;

@Component
public class LocationServiceClient {

    private final RestClient restClient;

    public LocationServiceClient(
            @Value("${ridex.services.location.base-url}")
            String locationServiceUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(locationServiceUrl)
                .build();
    }

    public List<NearbyDriverResponse> findNearbyDrivers(
            double latitude,
            double longitude,
            double radiusKm
    ) {
        List<NearbyDriverResponse> response =
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v1/internal/drivers/nearby")
                                .queryParam("latitude", latitude)
                                .queryParam("longitude", longitude)
                                .queryParam("radiusKm", radiusKm)
                                .build()
                        )
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<>() {}
                        );

        return response == null
                ? List.of()
                : response;
    }
}