package com.ridex.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridex.payment.dto.PaymentMethod;
import com.ridex.payment.dto.response.PaymentResponse;
import com.ridex.payment.dto.PaymentStatus;
import com.ridex.payment.exception.GlobalExceptionHandler;
import com.ridex.payment.service.PaymentService;
import com.ridex.payment.service.PaymentWebhookService;
import com.ridex.payment.service.PaymentWebhookVerifier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        ObjectMapper objectMapper;

        @MockitoBean
        PaymentService paymentService;

        @MockitoBean
        PaymentWebhookService paymentWebhookService;

        @MockitoBean
        PaymentWebhookVerifier paymentWebhookVerifier;

        @Test
        void getsPaymentForTrip() throws Exception {
                UUID tripId = UUID.randomUUID();

                when(paymentService.getPaymentForAuthenticatedRider(tripId))
                                .thenReturn(new PaymentResponse(
                                                UUID.randomUUID(), tripId, UUID.randomUUID(),
                                                new BigDecimal("100.00"), "INR",
                                                PaymentStatus.PENDING, null, null,
                                                Instant.now(), Instant.now()));

                mockMvc.perform(get("/api/v1/payments/trips/{tripId}", tripId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("PENDING"))
                                .andExpect(jsonPath("$.currency").value("INR"));

                verify(paymentService).getPaymentForAuthenticatedRider(tripId);
        }

        @Test
        void initiatesPayment() throws Exception {
                UUID paymentId = UUID.randomUUID();

                when(paymentService.initiatePayment(paymentId, PaymentMethod.CARD))
                                .thenReturn(new PaymentResponse(
                                                paymentId, UUID.randomUUID(), UUID.randomUUID(),
                                                new BigDecimal("100.00"), "INR",
                                                PaymentStatus.PENDING, PaymentMethod.CARD, "provider-1",
                                                Instant.now(), Instant.now()));

                mockMvc.perform(post("/api/v1/payments/{id}/initiate", paymentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"paymentMethod":"CARD"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.paymentMethod").value("CARD"));

                verify(paymentService).initiatePayment(paymentId, PaymentMethod.CARD);
        }

        @Test
        void rejectsInitiationWhenPaymentMethodIsMissing() throws Exception {
                mockMvc.perform(post("/api/v1/payments/{id}/initiate", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(paymentService);
        }

        @Test
        void acceptsWebhookWithValidSignature() throws Exception {
                when(paymentWebhookVerifier.verify(anyString(), eq("mock-valid-signature")))
                                .thenReturn(true);

                mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                                .header("X-Payment-Signature", "mock-valid-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "providerPaymentId":"provider-1",
                                                  "idempotencyKey":"idem-1",
                                                  "status":"SUCCESS",
                                                  "eventId":"evt-1",
                                                  "failureReason":null
                                                }
                                                """))
                                .andExpect(status().isOk());

                verify(paymentWebhookService).process(any());
        }

        @Test
        void rejectsWebhookWithInvalidSignature() throws Exception {
                when(paymentWebhookVerifier.verify(anyString(), anyString())).thenReturn(false);

                mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                                .header("X-Payment-Signature", "bad-signature")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(paymentWebhookService);
        }
}
