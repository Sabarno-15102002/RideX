package com.ridex.matching.client;

import java.util.UUID;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ridex.matching.dto.request.ReserveDriverRequest;
import com.ridex.matching.dto.response.DriverReservationResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DriverServiceClient {

    private final LoadBalancerClient loadBalancerClient;
    private final RestClient restClient;

    public DriverServiceClient(
            LoadBalancerClient loadBalancerClient
    ) {
        this.loadBalancerClient = loadBalancerClient;
        this.restClient = RestClient.builder().build();
    }

    public boolean reserveDriver(UUID driverId, UUID tripId) {

        ServiceInstance instance =
                loadBalancerClient.choose("driver-service");

        if (instance == null) {
            throw new IllegalStateException(
                    "No available driver-service instance"
            );
        }

        DriverReservationResponse response = restClient
                .post()
                .uri(
                        instance.getUri() +
                        "/api/v1/internal/drivers/{driverId}/reservations",
                        driverId
                )
                .body(new ReserveDriverRequest(tripId))
                .retrieve()
                .body(DriverReservationResponse.class);

        log.info(
                "Reserve driver response: {}, instance={}",
                response,
                instance.getInstanceId()
        );

        return response != null && response.reserved();
    }
}