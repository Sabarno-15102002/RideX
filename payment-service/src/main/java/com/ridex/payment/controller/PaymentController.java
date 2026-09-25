package com.ridex.payment.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridex.payment.dto.PaymentProviderWebhook;
import com.ridex.payment.dto.request.InitiatePaymentRequest;
import com.ridex.payment.dto.response.PaymentResponse;
import com.ridex.payment.exception.InvalidWebhookSignatureException;
import com.ridex.payment.service.PaymentService;
import com.ridex.payment.service.PaymentWebhookService;
import com.ridex.payment.service.PaymentWebhookVerifier;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentWebhookService paymentWebhookService;
    private final PaymentWebhookVerifier paymentWebhookVerifier;
    private final ObjectMapper objectMapper;

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<PaymentResponse> getPaymentByTrip(
            @PathVariable UUID tripId) {

        return ResponseEntity.ok(
                paymentService.getPaymentForAuthenticatedRider(tripId));
    }

    @PostMapping("/webhooks/{provider}")
    @ResponseStatus(HttpStatus.OK)
    public void handleWebhook(
            @PathVariable String provider,
            @RequestHeader("X-Payment-Signature") String signature,
            @RequestBody String payload) throws JsonProcessingException {

        if (!paymentWebhookVerifier.verify(payload, signature)) {
            throw new InvalidWebhookSignatureException(
                    "Invalid payment provider signature");
        }

        PaymentProviderWebhook webhook = objectMapper.readValue(
                payload,
                PaymentProviderWebhook.class);

        paymentWebhookService.process(webhook);
    }

    @PostMapping("/{paymentId}/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody InitiatePaymentRequest request) {

        return ResponseEntity.ok(
                paymentService.initiatePayment(
                        paymentId,
                        request.paymentMethod()
                )
        );
    }
}