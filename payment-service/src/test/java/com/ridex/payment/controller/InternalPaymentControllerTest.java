
package com.ridex.payment.controller;

import com.ridex.payment.config.JwtAuthenticationFilter;
import com.ridex.payment.dto.PaymentStatus;
import com.ridex.payment.dto.response.PaymentResponse;
import com.ridex.payment.service.PaymentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InternalPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalPaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PaymentService paymentService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    private PaymentResponse response(UUID id, PaymentStatus status) {
        return new PaymentResponse(
                id, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), "INR", status, null, null,
                Instant.now(), Instant.now());
    }

    @Test
    void processesPayment() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.processPayment(id)).thenReturn(response(id, PaymentStatus.SUCCESS));

        mockMvc.perform(post("/api/v1/internal/payments/{id}/process", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentService).processPayment(id);
    }

    @Test
    void refundsPayment() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.refund(id)).thenReturn(response(id, PaymentStatus.REFUNDED));

        mockMvc.perform(post("/api/v1/internal/payments/{id}/refund", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        verify(paymentService).refund(id);
    }
}
