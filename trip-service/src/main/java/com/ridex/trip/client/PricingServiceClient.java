package com.ridex.trip.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ridex.trip.dto.response.FareQuoteResponse;

@Component
public class PricingServiceClient {

    private final RestClient pricingRestClient;

    @Autowired
    public PricingServiceClient(
            @Value("${ridex.services.pricing.base-url}")
            String pricingServiceUrl
    ) {
        this(pricingServiceUrl, Duration.ofSeconds(3));
    }

    PricingServiceClient(
            String pricingServiceUrl,
            Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        this.pricingRestClient = RestClient.builder()
                .baseUrl(pricingServiceUrl)
                .requestFactory(requestFactory)
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