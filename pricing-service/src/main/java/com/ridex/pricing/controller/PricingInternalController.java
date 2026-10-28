package com.ridex.pricing.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridex.pricing.dto.response.FareQuoteResponse;
import com.ridex.pricing.service.FareCalculationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/pricing")
@RequiredArgsConstructor
public class PricingInternalController {
    private final FareCalculationService fareCalculationService;

    @GetMapping("/trips/{tripId}")
    public FareQuoteResponse getFareQuote(@PathVariable UUID tripId) {

        return fareCalculationService.getFareQuote(tripId);
    }
}
