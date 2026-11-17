package com.ridex.matching.client;

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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DriverServiceClientTest {

        private LoadBalancerClient loadBalancerClient;
        private HttpServer server;
        private DriverServiceClient driverServiceClient;

        private UUID driverId;
        private UUID tripId;

        private AtomicInteger requestCount;
        private AtomicReference<String> requestPath;
        private AtomicReference<String> requestBody;

        @BeforeEach
        void setUp() throws IOException {
                loadBalancerClient = mock(LoadBalancerClient.class);

                driverId = UUID.randomUUID();
                tripId = UUID.randomUUID();

                requestCount = new AtomicInteger();
                requestPath = new AtomicReference<>();
                requestBody = new AtomicReference<>();

                server = HttpServer.create(
                                new InetSocketAddress("127.0.0.1", 0), 0);

                server.createContext("/", exchange -> {
                        requestCount.incrementAndGet();

                        requestPath.set(
                                        exchange.getRequestURI().toString());

                        requestBody.set(
                                        new String(
                                                        exchange.getRequestBody().readAllBytes(),
                                                        StandardCharsets.UTF_8));

                        byte[] response = """
                                        {"reserved":true}
                                        """.getBytes(StandardCharsets.UTF_8);

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

                when(loadBalancerClient.choose("driver-service"))
                                .thenReturn(instance);

                driverServiceClient = new DriverServiceClient(
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
        void reserveDriver_shouldReturnTrueWhenReservationSucceeds() {
                boolean result = driverServiceClient.reserveDriver(
                                driverId, tripId);

                assertThat(result).isTrue();
                assertThat(requestCount.get()).isEqualTo(1);
        }

        @Test
        void reserveDriver_shouldSendCorrectEndpointAndRequestBody() {
                driverServiceClient.reserveDriver(driverId, tripId);

                assertThat(requestPath.get())
                                .isEqualTo(
                                                "/api/v1/internal/drivers/" + driverId + "/reservations");

                assertThat(requestBody.get())
                                .contains(tripId.toString());
        }

        @Test
        void reserveDriver_shouldReturnFalseWhenReservationIsRejected()
                        throws IOException {

                server.removeContext("/");

                server.createContext("/", exchange -> {
                        byte[] response = """
                                        {"reserved":false}
                                        """.getBytes(StandardCharsets.UTF_8);

                        exchange.getResponseHeaders().add(
                                        "Content-Type", "application/json");

                        exchange.sendResponseHeaders(200, response.length);
                        exchange.getResponseBody().write(response);
                        exchange.close();
                });

                boolean result = driverServiceClient.reserveDriver(
                                driverId, tripId);

                assertThat(result).isFalse();
        }

        @Test
        void reserveDriver_shouldReturnFalseWhenResponseBodyIsEmpty()
                        throws IOException {

                server.removeContext("/");

                server.createContext("/", exchange -> {
                        exchange.sendResponseHeaders(204, -1);
                        exchange.close();
                });

                boolean result = driverServiceClient.reserveDriver(
                                driverId, tripId);

                assertThat(result).isFalse();
        }

        @Test
        void reserveDriver_shouldReturnFalseWhenHttpRequestFails()
                        throws IOException {

                server.removeContext("/");

                server.createContext("/", exchange -> {
                        byte[] response = "Internal Server Error"
                                        .getBytes(StandardCharsets.UTF_8);

                        exchange.sendResponseHeaders(500, response.length);
                        exchange.getResponseBody().write(response);
                        exchange.close();
                });

                boolean result = driverServiceClient.reserveDriver(
                                driverId, tripId);

                assertThat(result).isFalse();
        }

        @Test
        void reserveDriver_shouldThrowWhenNoServiceInstanceIsAvailable() {
                when(loadBalancerClient.choose("driver-service"))
                                .thenReturn(null);

                assertThrows(
                                IllegalStateException.class,
                                () -> driverServiceClient.reserveDriver(
                                                driverId, tripId));
        }
}
