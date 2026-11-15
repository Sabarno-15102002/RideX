
package com.ridex.trip.client;

import com.ridex.trip.dto.response.FareQuoteResponse;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceClientTest {

        private HttpServer server;
        private PricingServiceClient pricingServiceClient;

        private final AtomicReference<String> requestPath = new AtomicReference<>();

        private final AtomicReference<String> requestMethod = new AtomicReference<>();

        private volatile int responseStatus;
        private volatile String responseBody;
        private volatile long responseDelayMillis;

        private static final String VALID_RESPONSE = """
                        {
                          "tripId": "%s",
                          "riderId": "%s",
                          "baseFare": 30.00,
                          "distanceKm": 5.00,
                          "durationMinutes": 15.00,
                          "distanceFare": 20.00,
                          "durationFare": 10.00,
                          "totalFare": 60.00,
                          "currency": "INR"
                        }
                        """;

        @BeforeEach
        void setUp() throws Exception {
                server = HttpServer.create(
                                new InetSocketAddress("localhost", 0), 0);

                server.createContext("/api/v1/pricing/trips", exchange -> {
                        requestPath.set(exchange.getRequestURI().getPath());
                        requestMethod.set(exchange.getRequestMethod());

                        try {
                                if (responseDelayMillis > 0) {
                                        Thread.sleep(responseDelayMillis);
                                }

                                exchange.getResponseHeaders().set(
                                                "Content-Type", "application/json");

                                byte[] body = responseBody.getBytes(
                                                StandardCharsets.UTF_8);

                                if (body.length == 0) {
                                        exchange.sendResponseHeaders(responseStatus, -1);
                                } else {
                                        exchange.sendResponseHeaders(
                                                        responseStatus, body.length);
                                        exchange.getResponseBody().write(body);
                                }
                        } catch (InterruptedException exception) {
                                Thread.currentThread().interrupt();
                        } catch (IOException ignored) {
                                // The client may have timed out and closed the connection.
                        } finally {
                                exchange.close();
                        }
                });

                server.start();

                responseStatus = 200;
                responseBody = "";
                responseDelayMillis = 0;

                String baseUrl = "http://localhost:"
                                + server.getAddress().getPort();

                pricingServiceClient = new PricingServiceClient(
                                baseUrl, Duration.ofMillis(300));
        }

        @AfterEach
        void tearDown() {
                if (server != null) {
                        server.stop(0);
                }
        }

        @Test
        void getFareQuote_shouldReturnFareQuote() {
                UUID tripId = UUID.randomUUID();
                UUID riderId = UUID.randomUUID();

                responseBody = VALID_RESPONSE.formatted(tripId, riderId);

                FareQuoteResponse response = pricingServiceClient.getFareQuote(tripId);

                assertNotNull(response);
                assertEquals(tripId, response.tripId());
                assertEquals(riderId, response.riderId());
                assertEquals(new BigDecimal("60.00"), response.totalFare());
                assertEquals("INR", response.currency());

                assertEquals(
                                "/api/v1/pricing/trips/" + tripId,
                                requestPath.get());

                assertEquals("GET", requestMethod.get());
        }

        @Test
        void getFareQuote_shouldThrowWhenPricingServiceReturns404() {
                responseStatus = 404;
                responseBody = "Trip not found";

                assertThrows(
                                RestClientResponseException.class,
                                () -> pricingServiceClient.getFareQuote(UUID.randomUUID()));
        }

        @Test
        void getFareQuote_shouldThrowWhenPricingServiceReturns500() {
                responseStatus = 500;
                responseBody = "Internal server error";

                assertThrows(
                                RestClientResponseException.class,
                                () -> pricingServiceClient.getFareQuote(UUID.randomUUID()));
        }

        @Test
        void getFareQuote_shouldThrowWhenRequestTimesOut() {
                responseDelayMillis = 1000;

                assertThrows(
                                ResourceAccessException.class,
                                () -> pricingServiceClient.getFareQuote(UUID.randomUUID()));
        }

        @Test
        void getFareQuote_shouldThrowWhenResponseJsonIsMalformed() {
                responseBody = "{ invalid json";

                assertThrows(
                                RestClientException.class,
                                () -> pricingServiceClient.getFareQuote(UUID.randomUUID()));
        }

        @Test
        void getFareQuote_shouldHandleEmptyResponseBody() {
                responseBody = "";

                FareQuoteResponse response = pricingServiceClient.getFareQuote(UUID.randomUUID());

                assertNull(response);
        }
}
