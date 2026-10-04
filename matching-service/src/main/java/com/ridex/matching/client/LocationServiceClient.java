package com.ridex.matching.client;

import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ridex.matching.dto.response.NearbyDriverResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LocationServiceClient {

        private final LoadBalancerClient loadBalancerClient;
    private final RestClient restClient;

    public LocationServiceClient(
            LoadBalancerClient loadBalancerClient
    ) {
        this.loadBalancerClient = loadBalancerClient;
        this.restClient = RestClient.builder().build();
    }

    public List<NearbyDriverResponse> findNearbyDrivers(
            double latitude,
            double longitude,
            double radiusKm
    ) {

        ServiceInstance instance =
                loadBalancerClient.choose("location-service");

        if (instance == null) {
            throw new IllegalStateException(
                    "No available location-service instance"
            );
        }

        List<NearbyDriverResponse> response =
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme(instance.getUri().getScheme())
                                .host(instance.getHost())
                                .port(instance.getPort())
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

        log.info(
                "Nearby drivers response: {}, instance={}",
                response,
                instance.getInstanceId()
        );

        return response == null
                ? List.of()
                : response;
    }
}