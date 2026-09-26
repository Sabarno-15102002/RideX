package com.ridex.trip.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.trip.dto.PaymentStatus;
import com.ridex.trip.entity.TripPayment;
import com.ridex.trip.event.event.PaymentFailedEvent;
import com.ridex.trip.event.event.PaymentRefundedEvent;
import com.ridex.trip.event.event.PaymentSucceededEvent;
import com.ridex.trip.repository.TripPaymentRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor 
public class TripPaymentProjection {

    private final TripPaymentRepository tripPaymentRepository;

    @Transactional
    public void project(PaymentSucceededEvent event) {
        TripPayment payment = tripPaymentRepository.findById(event.tripId())
                .orElseGet(() -> new TripPayment(
                        event.tripId(),
                        event.paymentId(),
                        event.riderId(),
                        event.amount(),
                        event.currency(),
                        PaymentStatus.SUCCESS,
                        event.paidAt()
                ));

        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        payment.markSuccess(event.paidAt());
        tripPaymentRepository.save(payment);
    }

    @Transactional
    public void project(PaymentFailedEvent event) {
        TripPayment payment = tripPaymentRepository.findById(event.tripId())
                .orElseGet(() -> new TripPayment(
                        event.tripId(),
                        event.paymentId(),
                        event.riderId(),
                        event.amount(),
                        event.currency(),
                        PaymentStatus.FAILED,
                        event.failedAt()
                ));

        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        payment.markFailed(event.failedAt());
        tripPaymentRepository.save(payment);
    }

    @Transactional
    public void project(PaymentRefundedEvent event) {
        TripPayment payment = tripPaymentRepository.findById(event.tripId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Payment projection not found for trip: "
                                        + event.tripId()
                        )
                );

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException(
                    "Cannot refund payment in status: "
                            + payment.getStatus()
            );
        }

        payment.markRefunded(event.refundedAt());
        tripPaymentRepository.save(payment);
    }
}