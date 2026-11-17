package com.ridex.matching.client;

import com.ridex.matching.dto.response.NearbyDriverResponse;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LocationServiceClientTest {

        private LoadBalancerClient loadBalancerClient;
        private HttpServer server;
        private LocationServiceClient locationServiceClient;

        private UUID driverId;
        private AtomicInteger requestCount;
        private AtomicReference<String> requestPath;

        @BeforeEach
        void setUp() throws IOException {
                loadBalancerClient = mock(LoadBalancerClient.class);

                driverId = UUID.randomUUID();
                requestCount = new AtomicInteger();
                requestPath = new AtomicReference<>();

                server = HttpServer.create(
                                new InetSocketAddress("127.0.0.1", 0), 0);

                server.createContext("/", exchange -> {
                        requestCount.incrementAndGet();
                        requestPath.set(
                                        exchange.getRequestURI().toString());

                        byte[] response = """
                                        [{"driverId":"%s","distanceKm":1.2}]
                                        """.formatted(driverId)
                                        .getBytes(StandardCharsets.UTF_8);

                        exchange.getResponseHeaders().add(
                                        "Content-Type", "application/json");

                        exchange.sendResponseHeaders(200, response.length);
                        exchange.getResponseBody().write(response);
                        exchange.close();
                });

                server.start();

                ServiceInstance instance = mock(ServiceInstance.class);

                when(instance.getUri()).thenReturn(
                                URI.create("http://127.0.0.1:" + server.getAddress().getPort()));

                when(instance.getHost()).thenReturn("127.0.0.1");
                when(instance.getPort()).thenReturn(server.getAddress().getPort());
                when(instance.getInstanceId()).thenReturn("location-test-instance");

                when(loadBalancerClient.choose("location-service"))
                                .thenReturn(instance);

                locationServiceClient = new LocationServiceClient(
                                loadBalancerClient,
                                RestClient.builder());
        }

        @AfterEach
        void tearDown() {
                if (server != null) {
                        server.stop(0);
                }
        }

        @Test
        void findNearbyDrivers_shouldReturnNearbyDrivers() {
                List<NearbyDriverResponse> result = locationServiceClient.findNearbyDrivers(
                                12.9716, 77.5946, 5.0);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).driverId()).isEqualTo(driverId);
                assertThat(result.get(0).distanceKm()).isEqualTo(1.2);

                assertThat(requestCount.get()).isEqualTo(1);
        }

        @Test
        void findNearbyDrivers_shouldUseCorrectEndpointAndParameters() {
                locationServiceClient.findNearbyDrivers(
                                12.9716, 77.5946, 5.0);

                assertThat(requestPath.get())
                                .startsWith("/api/v1/internal/drivers/nearby?");

                assertThat(requestPath.get())
                                .contains("latitude=12.9716");

                assertThat(requestPath.get())
                                .contains("longitude=77.5946");

                assertThat(requestPath.get())
                                .contains("radiusKm=5.0");
        }

        @Test
        void findNearbyDrivers_shouldReturnEmptyListWhenResponseBodyIsEmpty()
                        throws IOException {

                server.removeContext("/");

                server.createContext("/", exchange -> {
                        requestCount.incrementAndGet();
                        exchange.sendResponseHeaders(204, -1);
                        exchange.close();
                });

                List<NearbyDriverResponse> result = locationServiceClient.findNearbyDrivers(
                                12.9716, 77.5946, 5.0);

                assertThat(result).isEmpty();
        }

        @Test
        void findNearbyDrivers_shouldReturnEmptyListWhenHttpRequestFails()
                        throws IOException {

                server.removeContext("/");

                server.createContext("/", exchange -> {
                        requestCount.incrementAndGet();

                        byte[] response = "Internal Server Error"
                                        .getBytes(StandardCharsets.UTF_8);

                        exchange.sendResponseHeaders(500, response.length);
                        exchange.getResponseBody().write(response);
                        exchange.close();
                });

                List<NearbyDriverResponse> result = locationServiceClient.findNearbyDrivers(
                                12.9716, 77.5946, 5.0);

                assertThat(result).isEmpty();
                assertThat(requestCount.get()).isEqualTo(1);
        }

        @Test
        void findNearbyDrivers_shouldThrowWhenNoServiceInstanceIsAvailable() {
                when(loadBalancerClient.choose("location-service"))
                                .thenReturn(null);

                assertThrows(
                                IllegalStateException.class,
                                () -> locationServiceClient.findNearbyDrivers(
                                                12.9716, 77.5946, 5.0));
        }
}
