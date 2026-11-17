package com.ridex.driver.config;

import com.ridex.driver.controller.DriverController;
import com.ridex.driver.controller.DriverReservationController;
import com.ridex.driver.service.DriverOnboardingService;
import com.ridex.driver.service.DriverReservationService;
import com.ridex.driver.service.DriverService;
import com.ridex.driver.service.DriverTripService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        DriverController.class,
        DriverReservationController.class
}, properties = {
        "jwt.secret=driver-service-integration-test-secret-key-at-least-32-bytes"
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class SecurityIntegrationTest {

    private static final String SECRET = "driver-service-integration-test-secret-key-at-least-32-bytes";

    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            SECRET.getBytes(StandardCharsets.UTF_8));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DriverOnboardingService driverOnboardingService;

    @MockitoBean
    private DriverService driverService;

    @MockitoBean
    private DriverTripService driverTripService;

    @MockitoBean
    private DriverReservationService driverReservationService;

    @Test
    void protectedEndpoint_withoutToken_shouldBeForbidden()
            throws Exception {

        UUID tripId = UUID.randomUUID();

        mockMvc.perform(post(
                "/api/v1/drivers/me/trips/{tripId}/accept",
                tripId))
                .andExpect(status().isForbidden());

        verifyNoInteractions(driverTripService);
    }

    @Test
    void protectedEndpoint_withInvalidToken_shouldBeForbidden()
            throws Exception {

        UUID tripId = UUID.randomUUID();

        mockMvc.perform(post(
                "/api/v1/drivers/me/trips/{tripId}/accept",
                tripId)
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(driverTripService);
    }

    @Test
    void driverEndpoint_withDriverRole_shouldAllowAccess()
            throws Exception {

        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        String token = createToken(driverId, "DRIVER");

        mockMvc.perform(post(
                "/api/v1/drivers/me/trips/{tripId}/accept",
                tripId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(driverTripService).acceptTrip(driverId, tripId);
    }

    @Test
    void driverEndpoint_withRiderRole_shouldForbidAccess()
            throws Exception {

        UUID riderId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        String token = createToken(riderId, "RIDER");

        mockMvc.perform(post(
                "/api/v1/drivers/me/trips/{tripId}/accept",
                tripId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(driverTripService);
    }

    @Test
    void internalReservationEndpoint_isCurrentlyPublic()
            throws Exception {

        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        when(driverReservationService.reserveDriver(driverId, tripId))
                .thenReturn(true);

        mockMvc.perform(post(
                "/api/v1/internal/drivers/{driverId}/reservations",
                driverId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "tripId": "%s"
                        }
                        """.formatted(tripId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserved").value(true));

        verify(driverReservationService).reserveDriver(driverId, tripId);
    }

    private String createToken(UUID userId, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .signWith(SECRET_KEY)
                .compact();
    }
}
