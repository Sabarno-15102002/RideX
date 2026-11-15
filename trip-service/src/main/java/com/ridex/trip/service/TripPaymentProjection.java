package com.ridex.trip.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.trip.dto.PaymentStatus;
import com.ridex.trip.entity.TripPayment;
import com.ridex.trip.event.event.PaymentFailedEvent;
import com.ridex.trip.event.event.PaymentRefundedEvent;
import com.ridex.trip.event.event.PaymentSucceededEvent;
import com.ridex.trip.exception.IllegalOperationException;
import com.ridex.trip.exception.ResourceNotFoundException;
import com.ridex.trip.repository.TripPaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripPaymentProjection {

        private final TripPaymentRepository tripPaymentRepository;

        @CacheEvict(cacheNames = "tripPayment", key = "#event.tripId() + ':' + #event.riderId()")
        @Transactional
        public void project(PaymentSucceededEvent event) {

                TripPayment payment = tripPaymentRepository.findById(event.tripId())
                                .orElseGet(() -> new TripPayment(
                                                event.tripId(),
                                                event.paymentId(),
                                                event.riderId(),
                                                event.amount(),
                                                event.currency(),
                                                PaymentStatus.PENDING,
                                                event.paidAt()));

                if (payment.getStatus() == PaymentStatus.SUCCESS
                                || payment.getStatus() == PaymentStatus.REFUNDED) {
                        return;
                }

                payment.markSuccess(event.paidAt());

                tripPaymentRepository.save(payment);
        }

        @CacheEvict(cacheNames = "tripPayment", key = "#event.tripId() + ':' + #event.riderId()")
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
                                                event.failedAt()));

                if (payment.getStatus() == PaymentStatus.SUCCESS
                                || payment.getStatus() == PaymentStatus.REFUNDED) {
                        return;
                }

                payment.markFailed(event.failedAt());
                tripPaymentRepository.save(payment);
        }

        @CacheEvict(cacheNames = "tripPayment", key = "#p0.tripId() + ':' + #p0.riderId()")
        @Transactional
        public void project(PaymentRefundedEvent event) {
                TripPayment payment = tripPaymentRepository.findById(event.tripId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Payment not found for trip: " + event.tripId()));

                if (payment.getStatus() == PaymentStatus.REFUNDED) {
                        return;
                }

                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                        throw new IllegalOperationException(
                                        "Cannot refund payment with status: " + payment.getStatus());
                }

                payment.markRefunded(event.refundedAt());
                tripPaymentRepository.save(payment);
        }
}