package com.ridex.pricing.controller;

import com.ridex.pricing.dto.response.FareQuoteResponse;
import com.ridex.pricing.service.FareCalculationService;
import org.junit.jupiter.api.Test;
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

@WebMvcTest(PricingInternalController.class)
@AutoConfigureMockMvc(addFilters = false)
class PricingInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FareCalculationService fareCalculationService;

    @Test
    void shouldReturnFareQuoteForInternalRequest() throws Exception {
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

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

        when(fareCalculationService.getFareQuote(tripId))
                .thenReturn(response);

        mockMvc.perform(
                get("/api/v1/internal/pricing/trips/{tripId}", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(tripId.toString()))
                .andExpect(jsonPath("$.totalFare").value(240.00));

        verify(fareCalculationService).getFareQuote(tripId);
    }

    @Test
    void shouldReturnNotFoundWhenInternalQuoteDoesNotExist() throws Exception {
        UUID tripId = UUID.randomUUID();

        when(fareCalculationService.getFareQuote(tripId))
                .thenThrow(new com.ridex.pricing.exception.ResourceNotFoundException(
                        "Fare quote not found"));

        mockMvc.perform(
                get("/api/v1/internal/pricing/trips/{tripId}", tripId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
