package com.ridex.payment.service.Impl;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.payment.config.SecurityUtils;
import com.ridex.payment.dto.PaymentAttemptStatus;
import com.ridex.payment.dto.PaymentMethod;
import com.ridex.payment.dto.PaymentStatus;
import com.ridex.payment.dto.response.FareQuoteResponse;
import com.ridex.payment.dto.response.PaymentResponse;
import com.ridex.payment.dto.response.PaymentResult;
import com.ridex.payment.entity.Payment;
import com.ridex.payment.entity.PaymentAttempt;
import com.ridex.payment.event.PaymentFailedEvent;
import com.ridex.payment.event.PaymentRefundedEvent;
import com.ridex.payment.event.PaymentSucceededEvent;
import com.ridex.payment.event.TripCompletedEvent;
import com.ridex.payment.exception.InvalidPaymentStateException;
import com.ridex.payment.exception.PaymentProviderException;
import com.ridex.payment.repository.PaymentAttemptRepository;
import com.ridex.payment.repository.PaymentRepository;
import com.ridex.payment.service.PaymentProvider;
import com.ridex.payment.service.PaymentService;
import com.ridex.payment.service.RiderIdentityService;
import com.ridex.payment.service.provider.PaymentProviderFactory;
import com.ridex.payment.service.util.OutboxEventService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

        private final PaymentRepository paymentRepository;
        private final RiderIdentityService riderIdentityService;
        private final PaymentProvider paymentProvider;
        private final PaymentProviderFactory paymentProviderFactory;
        private final PaymentAttemptRepository paymentAttemptRepository;
        private final OutboxEventService outboxEventService;

        @Override
        @Transactional
        public PaymentResponse createPendingPayment(TripCompletedEvent event, FareQuoteResponse fareQuote) {

                Optional<Payment> existing = paymentRepository.findByTripId(event.tripId());

                if (existing.isPresent()) {
                        return toResponse(existing.get());
                }

                Payment payment = new Payment();

                payment.setId(UUID.randomUUID());
                payment.setTripId(event.tripId());
                payment.setRiderId(event.riderId());

                payment.setAmount(fareQuote.totalFare());
                payment.setCurrency(fareQuote.currency());

                payment.setStatus(PaymentStatus.PENDING);
                payment.setPaymentMethod(null);
                payment.setIdempotencyKey("trip:" + event.tripId());

                Instant now = Instant.now();

                payment.setCreatedAt(now);
                payment.setUpdatedAt(now);

                return toResponse(paymentRepository.save(payment));
        }

        @Override
        @Transactional
        public PaymentResponse refund(UUID paymentId) {
                Payment payment = getPayment(paymentId);

                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                        throw new InvalidPaymentStateException(
                                        "Only successful payments can be refunded");
                }

                PaymentResult result = paymentProvider.refundPayment(payment);

                if (result.successful()) {
                        payment.setStatus(PaymentStatus.REFUNDED);
                        payment.setUpdatedAt(Instant.now());

                        PaymentRefundedEvent event = new PaymentRefundedEvent(
                                UUID.randomUUID(),
                                paymentId,
                                payment.getTripId(),
                                payment.getRiderId(),
                                payment.getAmount(),
                                payment.getCurrency(),
                                Instant.now()
                        );

                        outboxEventService.savePaymentRefundedEvent(event);
                } else {
                        throw new PaymentProviderException(
                                        result.failureReason());
                }

                return toResponse(paymentRepository.save(payment));
        }

        @Override
        @Transactional
        public PaymentResponse processPayment(UUID paymentId) {

                Payment payment = getPayment(paymentId);

                if (payment.getStatus() != PaymentStatus.PENDING) {
                        throw new InvalidPaymentStateException(
                                        "Payment cannot be processed from status "
                                                        + payment.getStatus());
                }

                PaymentResult result = paymentProvider.createPayment(payment);

                if (result.successful()) {
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setProviderPaymentId(result.providerPaymentId());

                        PaymentSucceededEvent event = new PaymentSucceededEvent(
                                UUID.randomUUID(),
                                paymentId,
                                payment.getTripId(),
                                payment.getRiderId(),
                                payment.getAmount(),
                                payment.getCurrency(),
                                Instant.now()
                        );

                        outboxEventService.savePaymentSucceededEvent(event);
                } else {
                        payment.setStatus(PaymentStatus.FAILED);

                        PaymentFailedEvent event = new PaymentFailedEvent(
                                UUID.randomUUID(),
                                paymentId,
                                payment.getTripId(),
                                payment.getRiderId(),
                                payment.getAmount(),
                                payment.getCurrency(),
                                Instant.now()
                        );

                        outboxEventService.savePaymentFailedEvent(event);
                }

                payment.setUpdatedAt(Instant.now());

                return toResponse(paymentRepository.save(payment));
        }

        private Payment getPayment(UUID paymentId) {
                return paymentRepository.findById(paymentId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Payment details not found with id:" + paymentId));
        }

        private PaymentResponse toResponse(Payment payment) {
                return new PaymentResponse(
                                payment.getId(),
                                payment.getTripId(),
                                payment.getRiderId(),
                                payment.getAmount(),
                                payment.getCurrency(),
                                payment.getStatus(),
                                payment.getPaymentMethod(),
                                payment.getProviderPaymentId(),
                                payment.getCreatedAt(),
                                payment.getUpdatedAt());
        }

        @Override
        public PaymentResponse getPaymentForAuthenticatedRider(UUID tripId) {
                UUID userId = SecurityUtils.getCurrentUserId();
                UUID riderId = riderIdentityService.getRiderIdByUserId(userId);

                Payment payment = paymentRepository
                                .findByRiderIdAndTripId(riderId, tripId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Payment not found for trip: " + tripId));

                return toResponse(payment);
        }

        @Override
        @Transactional
        public PaymentResponse initiatePayment(UUID paymentId, PaymentMethod paymentMethod) {

                UUID userId = SecurityUtils.getCurrentUserId();

                UUID riderId = riderIdentityService.getRiderIdByUserId(userId);

                Payment payment = paymentRepository
                                .findByIdForUpdate(paymentId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Payment not found: " + paymentId));

                if (!payment.getRiderId().equals(riderId)) {
                        throw new ResourceNotFoundException(
                                        "Payment not found: " + paymentId);
                }

                if (payment.getStatus() != PaymentStatus.PENDING) {
                        throw new InvalidPaymentStateException(
                                        "Payment cannot be initiated from status "
                                                        + payment.getStatus());
                }

                payment.setPaymentMethod(paymentMethod);
                payment.setUpdatedAt(Instant.now());

                PaymentAttempt previousAttempt = paymentAttemptRepository
                                .findTopByPaymentIdOrderByAttemptNumberDesc(
                                                paymentId)
                                .orElse(null);

                int attemptNumber = previousAttempt == null
                                ? 1
                                : previousAttempt.getAttemptNumber() + 1;

                String idempotencyKey = "payment:" + paymentId
                                + ":attempt:" + attemptNumber;

                PaymentAttempt attempt = new PaymentAttempt();

                attempt.setId(UUID.randomUUID());
                attempt.setPaymentId(paymentId);
                attempt.setAttemptNumber(attemptNumber);
                attempt.setIdempotencyKey(idempotencyKey);
                attempt.setStatus(PaymentAttemptStatus.CREATED);

                Instant now = Instant.now();

                attempt.setCreatedAt(now);
                attempt.setUpdatedAt(now);

                paymentAttemptRepository.save(attempt);

                PaymentProvider provider = paymentProviderFactory.getProvider();

                attempt.setStatus(PaymentAttemptStatus.PROCESSING);
                attempt.setUpdatedAt(Instant.now());

                paymentAttemptRepository.save(attempt);

                PaymentResult result = provider.createPayment(payment);

                if (!result.successful()) {

                        attempt.setStatus(PaymentAttemptStatus.FAILED);
                        attempt.setFailureReason(
                                        result.failureReason());
                        attempt.setUpdatedAt(Instant.now());

                        paymentAttemptRepository.save(attempt);

                        throw new PaymentProviderException(
                                        result.failureReason());
                }

                attempt.setProviderPaymentId(
                                result.providerPaymentId());

                attempt.setStatus(PaymentAttemptStatus.PROCESSING);
                attempt.setUpdatedAt(Instant.now());

                paymentAttemptRepository.save(attempt);

                payment.setProviderPaymentId(
                                result.providerPaymentId());

                payment.setUpdatedAt(Instant.now());

                Payment savedPayment = paymentRepository.save(payment);

                return toResponse(savedPayment);
        }
}
