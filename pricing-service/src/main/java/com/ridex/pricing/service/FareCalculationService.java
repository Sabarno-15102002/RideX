package com.ridex.pricing.service;

import java.util.UUID;

import com.ridex.pricing.dto.response.FareQuoteResponse;
import com.ridex.pricing.entity.FareQuote;
import com.ridex.pricing.event.TripCompletedEvent;


public interface FareCalculationService {

    FareQuote createFareQuote(TripCompletedEvent event);

    FareQuoteResponse getFareQuote(UUID tripId);

}
