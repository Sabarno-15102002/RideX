package com.ridex.pricing.controller;

import com.ridex.pricing.config.SecurityUtils;
import com.ridex.pricing.dto.response.FareQuoteResponse;
import com.ridex.pricing.service.FareCalculationService;
import com.ridex.pricing.service.RiderIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PricingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PricingControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private FareCalculationService fareCalculationService;

        @MockitoBean
        private RiderIdentityService riderIdentityService;

        private UUID userId;
        private UUID riderId;
        private UUID tripId;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                riderId = UUID.randomUUID();
                tripId = UUID.randomUUID();
        }

        @Test
        void shouldReturnFareQuoteForAuthenticatedRider() throws Exception {
                FareQuoteResponse response = new FareQuoteResponse(
                                tripId,
                                riderId,
                                new BigDecimal("50.00"),
                                new BigDecimal("10.00"),
                                new BigDecimal("20.00"),
                                new BigDecimal("150.00"),
                                new BigDecimal("40.00"),
                                new BigDecimal("240.00"),
                                "INR");

                when(riderIdentityService.getRiderIdByUserId(userId))
                                .thenReturn(riderId);
                when(fareCalculationService.getFareQuote(tripId, riderId))
                                .thenReturn(response);

                try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {

                        security.when(SecurityUtils::getCurrentUserId)
                                        .thenReturn(userId);

                        mockMvc.perform(get("/api/v1/pricing/trips/{tripId}", tripId))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.tripId").value(tripId.toString()))
                                        .andExpect(jsonPath("$.riderId").value(riderId.toString()))
                                        .andExpect(jsonPath("$.totalFare").value(240.00))
                                        .andExpect(jsonPath("$.currency").value("INR"));
                }

                verify(riderIdentityService).getRiderIdByUserId(userId);
                verify(fareCalculationService).getFareQuote(tripId, riderId);
        }

        @Test
        void shouldReturnNotFoundWhenFareQuoteDoesNotExist() throws Exception {
                when(riderIdentityService.getRiderIdByUserId(userId))
                                .thenReturn(riderId);
                when(fareCalculationService.getFareQuote(tripId, riderId))
                                .thenThrow(new com.ridex.pricing.exception.ResourceNotFoundException(
                                                "Fare quote not found"));

                try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {

                        security.when(SecurityUtils::getCurrentUserId)
                                        .thenReturn(userId);

                        mockMvc.perform(get("/api/v1/pricing/trips/{tripId}", tripId))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
                }
        }
}
