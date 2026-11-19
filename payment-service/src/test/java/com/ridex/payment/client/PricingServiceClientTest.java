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
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.web.client.RestClient;

class PricingServiceClientTest {

        private LoadBalancerClient loadBalancerClient;
        private HttpServer server;
        private PricingServiceClient client;

        @BeforeEach
        void setUp() throws IOException {
                loadBalancerClient = mock(LoadBalancerClient.class);

                server = HttpServer.create(new InetSocketAddress(0), 0);
                server.start();

                RestClient.Builder builder = RestClient.builder();

                client = new PricingServiceClient(
                                loadBalancerClient,
                                builder);

                ServiceInstance instance = new DefaultServiceInstance(
                                "pricing-service-1",
                                "pricing-service",
                                "localhost",
                                server.getAddress().getPort(),
                                false);

                when(loadBalancerClient.choose("pricing-service"))
                                .thenReturn(instance);
        }

        @AfterEach
        void tearDown() {
                if (server != null) {
                        server.stop(0);
                }
        }

        @Test
        void shouldReturnFareQuoteFromPricingService() {
                UUID tripId = UUID.randomUUID();
                UUID riderId = UUID.randomUUID();

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
                                """.formatted(tripId, riderId);

                server.createContext(
                                "/api/v1/internal/pricing/trips/" + tripId,
                                exchange -> {
                                        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                                        exchange.getResponseHeaders().add(
                                                        "Content-Type",
                                                        "application/json");
                                        exchange.sendResponseHeaders(200, bytes.length);
                                        exchange.getResponseBody().write(bytes);
                                        exchange.close();
                                });

                var result = client.getFareQuote(tripId);

                assertNotNull(result);
                assertEquals(tripId, result.tripId());
                assertEquals(riderId, result.riderId());
                assertEquals("INR", result.currency());
                assertEquals(0, result.totalFare().compareTo(
                                new java.math.BigDecimal("65.00")));
        }

        @Test
        void shouldThrowWhenPricingServiceInstanceIsUnavailable() {
                UUID tripId = UUID.randomUUID();

                when(loadBalancerClient.choose("pricing-service"))
                                .thenReturn(null);

                assertThrows(
                                IllegalStateException.class,
                                () -> client.getFareQuote(tripId));
        }

        @Test
        void shouldPropagateHttpFailure() {
                UUID tripId = UUID.randomUUID();

                server.createContext(
                                "/api/v1/internal/pricing/trips/" + tripId,
                                exchange -> {
                                        exchange.sendResponseHeaders(503, -1);
                                        exchange.close();
                                });

                assertThrows(
                                org.springframework.web.client.RestClientException.class,
                                () -> client.getFareQuote(tripId));
        }

        @Test
        void shouldRetryFailedHttpRequest() {
                UUID tripId = UUID.randomUUID();
                AtomicInteger attempts = new AtomicInteger();

                server.createContext(
                                "/api/v1/internal/pricing/trips/" + tripId,
                                exchange -> {
                                        int attempt = attempts.incrementAndGet();

                                        if (attempt == 1) {
                                                exchange.sendResponseHeaders(503, -1);
                                        } else {
                                                byte[] bytes = """
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
                                                                """.formatted(tripId, UUID.randomUUID())
                                                                .getBytes(StandardCharsets.UTF_8);

                                                exchange.getResponseHeaders().add(
                                                                "Content-Type",
                                                                "application/json");
                                                exchange.sendResponseHeaders(200, bytes.length);
                                                exchange.getResponseBody().write(bytes);
                                        }

                                        exchange.close();
                                });

                // This direct construction does not activate Spring's @Retryable proxy.
                // The retry assertion belongs in the Spring-proxy test below.
                assertThrows(
                                org.springframework.web.client.RestClientException.class,
                                () -> client.getFareQuote(tripId));

                assertEquals(1, attempts.get());
        }
}
