package com.ridex.rider.config;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.ridex.rider.controller.RiderController;
import com.ridex.rider.dto.response.RiderResponse;
import com.ridex.rider.service.RiderService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@WebMvcTest(RiderController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-that-is-at-least-32-bytes-long"
})
class SecurityConfigTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiderService riderService;

    @Test
    void protectedEndpoint_shouldRejectMissingToken()
            throws Exception {

        mockMvc.perform(get("/api/v1/riders/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(riderService);
    }

    @Test
    void protectedEndpoint_shouldAllowValidJwt()
            throws Exception {

        UUID userId = UUID.randomUUID();

        when(riderService.getMyProfile(userId))
                .thenReturn(new RiderResponse(
                        UUID.randomUUID(),
                        userId,
                        "John",
                        BigDecimal.valueOf(5.00)));

        mockMvc.perform(
                get("/api/v1/riders/me")
                        .header(
                                "Authorization",
                                "Bearer " + createToken(userId, "RIDER")))
                .andExpect(status().isOk());

        verify(riderService).getMyProfile(userId);
    }

    @Test
    void protectedEndpoint_shouldRejectInvalidJwt()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/riders/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(riderService);
    }

    @Test
    void actuatorPath_shouldNotRequireAuthentication()
            throws Exception {

        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        401,
                        result.getResponse().getStatus()));
    }

    private String createToken(UUID userId, String role) {
        SecretKey secretKey = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .signWith(secretKey)
                .compact();
    }
}
