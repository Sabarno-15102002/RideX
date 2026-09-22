package com.ridex.trip.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ridex.trip.dto.response.FareQuoteResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PricingServiceClient {

    private final RestClient.Builder pricingRestClientBuilder;

    @Value("${ridex.services.pricing.base-url}")
    private String baseUrl;

    public FareQuoteResponse getFareQuote(UUID tripId) {
        return pricingRestClientBuilder
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri("/api/v1/pricing/trips/{tripId}", tripId)
                .retrieve()
                .body(FareQuoteResponse.class);
    }
}