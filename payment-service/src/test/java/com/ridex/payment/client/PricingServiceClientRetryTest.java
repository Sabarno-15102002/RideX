package com.ridex.payment.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClient;

@SpringJUnitConfig(PricingServiceClientRetryTest.RetryTestConfig.class)
class PricingServiceClientRetryTest {

    @Configuration
    @EnableRetry
    static class RetryTestConfig {

        @Bean
        LoadBalancerClient loadBalancerClient() {
            return mock(LoadBalancerClient.class);
        }

        @Bean
        PricingServiceClient pricingServiceClient(
                LoadBalancerClient loadBalancerClient) {
            return new PricingServiceClient(
                    loadBalancerClient,
                    RestClient.builder());
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private PricingServiceClient client;

    @org.springframework.beans.factory.annotation.Autowired
    private LoadBalancerClient loadBalancerClient;

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();

        when(loadBalancerClient.choose("pricing-service"))
                .thenReturn(new DefaultServiceInstance(
                        "pricing-service-1",
                        "pricing-service",
                        "localhost",
                        server.getAddress().getPort(),
                        false));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldRetryOnceAfterRestClientException() {
        UUID tripId = UUID.randomUUID();
        AtomicInteger attempts = new AtomicInteger();

        server.createContext(
                "/api/v1/internal/pricing/trips/" + tripId,
                exchange -> {
                    int attempt = attempts.incrementAndGet();

                    if (attempt == 1) {
                        exchange.sendResponseHeaders(503, -1);
                    } else {
                        String response = """
                                {
                                  "tripId": "%s",
                                  "riderId": "%s",
                                  "baseFare": 30.00,
                                  "distanceKm": 5.00,
                                  "durationMinutes": 10.00,
                                  "distanceFare": 25.00,
                                  "durationFare": 10.00,
                                  "totalFare": 65.00,
                                  "currency": "INR"
                                }
                                """.formatted(tripId, UUID.randomUUID());

                        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                        exchange.getResponseHeaders().add(
                                "Content-Type",
                                "application/json");
                        exchange.sendResponseHeaders(200, bytes.length);
                        exchange.getResponseBody().write(bytes);
                    }

                    exchange.close();
                });

        var result = client.getFareQuote(tripId);

        assertNotNull(result);
        assertEquals(tripId, result.tripId());
        assertEquals(2, attempts.get());
    }

    @Test
    void shouldStopAfterConfiguredMaximumAttempts() {
        UUID tripId = UUID.randomUUID();
        AtomicInteger attempts = new AtomicInteger();

        server.createContext(
                "/api/v1/internal/pricing/trips/" + tripId,
                exchange -> {
                    attempts.incrementAndGet();
                    exchange.sendResponseHeaders(503, -1);
                    exchange.close();
                });

        assertThrows(
                org.springframework.web.client.RestClientException.class,
                () -> client.getFareQuote(tripId));

        assertEquals(2, attempts.get());
    }
}
