package com.ridex.matching.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ridex.matching.dto.request.ReserveDriverRequest;
import com.ridex.matching.dto.response.DriverReservationResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class DriverServiceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${ridex.services.driver.base-url}")
    private String baseUrl;

    public boolean reserveDriver(UUID driverId, UUID tripId) {

        DriverReservationResponse response = restClientBuilder
                .baseUrl(baseUrl)
                .build()
                .post()
                .uri("/api/v1/internal/drivers/{driverId}/reservations", driverId)
                .body(new ReserveDriverRequest(tripId))
                .retrieve()
                .body(DriverReservationResponse.class);

        return response != null && response.reserved();
    }
}