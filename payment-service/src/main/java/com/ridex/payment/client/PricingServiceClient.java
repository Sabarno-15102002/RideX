package com.ridex.payment.client;

import java.time.Duration;
import java.util.UUID;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ridex.payment.dto.response.FareQuoteResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PricingServiceClient {

    private final LoadBalancerClient loadBalancerClient;
        private final RestClient restClient;

        public PricingServiceClient(
                LoadBalancerClient loadBalancerClient,
                RestClient.Builder restClientBuilder) {

                this.loadBalancerClient = loadBalancerClient;

                JdkClientHttpRequestFactory requestFactory =
                        new JdkClientHttpRequestFactory();

                requestFactory.setReadTimeout(Duration.ofSeconds(2));

                this.restClient = restClientBuilder
                        .requestFactory(requestFactory)
                        .build();
        }

    @Retryable(retryFor = RestClientException.class, maxAttempts = 2, backoff = @Backoff(delay = 100))
    public FareQuoteResponse getFareQuote(UUID tripId) {

        ServiceInstance instance = loadBalancerClient.choose("pricing-service");

        if (instance == null) {
                throw new IllegalStateException(
                                "No available pricing-service instance");
        }

        FareQuoteResponse response = null;

        try{
            response = restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                                .scheme(instance.getUri().getScheme())
                                                .host(instance.getHost())
                                                .port(instance.getPort())
                                                .path("/api/v1/internal/pricing/trips/{tripId}")
                                                .build(tripId))
                                        .retrieve()
                                        .body(
                                            new ParameterizedTypeReference<>() {
                                        });
        }
        catch (RestClientException e) {
            log.error("Error while calling Pricing Service: {}", e.getMessage());
        }

        return response;
    }
}