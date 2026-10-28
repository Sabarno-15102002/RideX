package com.ridex.pricing.controller;

import com.ridex.pricing.config.SecurityUtils;
import com.ridex.pricing.dto.response.FareQuoteResponse;
import com.ridex.pricing.service.FareCalculationService;
import com.ridex.pricing.service.RiderIdentityService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final FareCalculationService fareCalculationService;
    private final RiderIdentityService riderIdentityService;

    @GetMapping("/trips/{tripId}")
    public FareQuoteResponse getFareQuote(@PathVariable UUID tripId) {

        UUID userId = SecurityUtils.getCurrentUserId();
        UUID riderId = riderIdentityService.getRiderIdByUserId(userId);
        return fareCalculationService.getFareQuote(tripId, riderId);
    }
}