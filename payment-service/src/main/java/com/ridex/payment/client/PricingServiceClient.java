package com.ridex.payment.client;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ridex.payment.dto.response.FareQuoteResponse;

import io.github.resilience4j.retry.annotation.Retry;

@Component
public class PricingServiceClient {

    private final RestClient restClient;

    public PricingServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Retry(name = "pricingService")
    public FareQuoteResponse getFareQuote(UUID tripId) {
        return restClient.get()
                .uri("/api/v1/pricing/trips/{tripId}", tripId)
                .retrieve()
                .onStatus(
                        status -> status.value() >= 500,
                        (request, response) -> {
                            throw new PricingServiceUnavailableException(
                                    "Pricing Service returned "
                                            + response.getStatusCode()
                            );
                        }
                )
                .body(FareQuoteResponse.class);
    }
}