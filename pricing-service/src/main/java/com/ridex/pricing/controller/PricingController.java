package com.ridex.pricing.controller;

import com.ridex.pricing.dto.response.FareQuoteResponse;
import com.ridex.pricing.service.FareCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final FareCalculationService fareCalculationService;

    @GetMapping("/trips/{tripId}")
    public FareQuoteResponse getFareQuote(@PathVariable UUID tripId) {

        return fareCalculationService.getFareQuote(tripId);
    }
}