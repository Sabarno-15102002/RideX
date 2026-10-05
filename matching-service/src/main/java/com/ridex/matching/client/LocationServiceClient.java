package com.ridex.matching.client;

import java.time.Duration;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ridex.matching.dto.response.NearbyDriverResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LocationServiceClient {

        private final LoadBalancerClient loadBalancerClient;
        private final RestClient restClient;

        public LocationServiceClient(
                        LoadBalancerClient loadBalancerClient) {
                this.loadBalancerClient = loadBalancerClient;

                JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();

                requestFactory.setReadTimeout(Duration.ofSeconds(2));

                this.restClient = RestClient.builder()
                                .requestFactory(requestFactory)
                                .build();
        }

        @Retryable(retryFor = RestClientException.class, maxAttempts = 2, backoff = @Backoff(delay = 100))
        public List<NearbyDriverResponse> findNearbyDrivers(
                        double latitude,
                        double longitude,
                        double radiusKm) {

                ServiceInstance instance = loadBalancerClient.choose("location-service");

                if (instance == null) {
                        throw new IllegalStateException(
                                        "No available location-service instance");
                }

                List<NearbyDriverResponse> response = null;

                try {
                        response = restClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .scheme(instance.getUri().getScheme())
                                                        .host(instance.getHost())
                                                        .port(instance.getPort())
                                                        .path("/api/v1/internal/drivers/nearby")
                                                        .queryParam("latitude", latitude)
                                                        .queryParam("longitude", longitude)
                                                        .queryParam("radiusKm", radiusKm)
                                                        .build())
                                        .retrieve()
                                        .body(
                                                        new ParameterizedTypeReference<>() {
                                                        });

                        log.info(
                                        "Nearby drivers response: {}, instance={}",
                                        response,
                                        instance.getInstanceId()

                        );
                } catch (RestClientException ex) {
                        log.error(
                                        "Location service call failed",
                                        ex);

                        return List.of();
                }

                return response == null
                                ? List.of()
                                : response;
        }
}