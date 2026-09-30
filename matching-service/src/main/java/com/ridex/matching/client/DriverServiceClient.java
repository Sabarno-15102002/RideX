package com.ridex.matching.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ridex.matching.dto.request.ReserveDriverRequest;
import com.ridex.matching.dto.response.DriverReservationResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DriverServiceClient {

    private final RestClient restClient;

    public DriverServiceClient(
            @Value("${ridex.services.driver.base-url}")
            String driverServiceUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(driverServiceUrl)
                .build();
    }

    public boolean reserveDriver(UUID driverId, UUID tripId) {

        DriverReservationResponse response = restClient
                .post()
                .uri("/api/v1/internal/drivers/{driverId}/reservations", driverId)
                .body(new ReserveDriverRequest(tripId))
                .retrieve()
                .body(DriverReservationResponse.class);

        log.info("Reserve driver response:" + response);
        return response != null && response.reserved();
    }
}