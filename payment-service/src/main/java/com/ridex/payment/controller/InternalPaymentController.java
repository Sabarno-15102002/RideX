package com.ridex.payment.controller;

import com.ridex.payment.dto.response.PaymentResponse;
import com.ridex.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{paymentId}/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable UUID paymentId) {

        return ResponseEntity.ok(
                paymentService.processPayment(paymentId)
        );
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refund(
            @PathVariable UUID paymentId) {

        return ResponseEntity.ok(
                paymentService.refund(paymentId)
        );
    }
}