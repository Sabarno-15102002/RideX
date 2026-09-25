package com.ridex.payment.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ridex.payment.dto.response.FareQuoteResponse;

@Component
public class PricingServiceClient {

    private final RestClient pricingRestClient;

    public PricingServiceClient(
            @Value("${ridex.services.pricing.base-url}")
            String pricingServiceUrl
    ) {
        this.pricingRestClient = RestClient.builder()
                .baseUrl(pricingServiceUrl)
                .build();
    }

    public FareQuoteResponse getFareQuote(UUID tripId) {
        return pricingRestClient
                .get()
                .uri("/api/v1/pricing/trips/{tripId}", tripId)
                .retrieve()
                .body(FareQuoteResponse.class);
    }
}