package com.ridex.trip.service.util;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ridex.trip.dto.response.TripPaymentResponse;
import com.ridex.trip.entity.TripPayment;
import com.ridex.trip.repository.TripPaymentRepository;

@Service
public class TripPaymentQueryService {

    private final TripPaymentRepository repository;

    public TripPaymentQueryService(TripPaymentRepository repository) {
        this.repository = repository;
    }

    public TripPaymentResponse getPaymentForTrip(
            UUID tripId,
            UUID authenticatedRiderId
    ) {
        TripPayment payment = repository.findById(tripId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found for trip: " + tripId
                        )
                );

        if (!payment.getRiderId().equals(authenticatedRiderId)) {
            throw new IllegalArgumentException(
                    "Rider does not own this trip"
            );
        }

        return new TripPaymentResponse(
                payment.getTripId(),
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getRefundedAt()
        );
    }
}