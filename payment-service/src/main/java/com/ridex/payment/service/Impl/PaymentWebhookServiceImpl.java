package com.ridex.payment.service.Impl;

import java.time.Instant;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.payment.dto.PaymentAttemptStatus;
import com.ridex.payment.dto.PaymentProviderWebhook;
import com.ridex.payment.dto.PaymentStatus;
import com.ridex.payment.entity.Payment;
import com.ridex.payment.entity.PaymentAttempt;
import com.ridex.payment.entity.PaymentProviderEvent;
import com.ridex.payment.exception.InvalidPaymentStateException;
import com.ridex.payment.repository.PaymentAttemptRepository;
import com.ridex.payment.repository.PaymentProviderEventRepository;
import com.ridex.payment.repository.PaymentRepository;
import com.ridex.payment.service.PaymentWebhookService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

        private final PaymentProviderEventRepository paymentProviderEventRepository;
        private final PaymentRepository paymentRepository;
        private final PaymentAttemptRepository paymentAttemptRepository;

        @Override
        @Transactional
        public void process(PaymentProviderWebhook webhook) {

                /*
                 * 1. Webhook idempotency.
                 *
                 * If this exact provider event was already processed,
                 * there is nothing more to do.
                 */
                if (paymentProviderEventRepository.existsById(webhook.eventId())) {
                        return;
                }

                /*
                 * 2. Lock the payment attempt.
                 *
                 * The providerPaymentId identifies the provider-side
                 * payment attempt, not merely the business Payment.
                 */
                PaymentAttempt attempt = paymentAttemptRepository
                                .findByProviderPaymentIdForUpdate(webhook.providerPaymentId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Payment attempt not found for provider payment: "
                                                                + webhook.providerPaymentId()));

                /*
                 * 3. Determine the target attempt status.
                 */
                PaymentAttemptStatus targetAttemptStatus = mapAttemptStatus(webhook.status());

                /*
                 * 4. Validate the attempt state transition.
                 */
                validateAttemptTransition(
                                attempt.getStatus(),
                                targetAttemptStatus);

                /*
                 * 5. Load and lock the parent Payment.
                 *
                 * This keeps Payment and PaymentAttempt consistent
                 * if concurrent provider events arrive.
                 */
                Payment payment = paymentRepository
                                .findByIdForUpdate(attempt.getPaymentId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Payment not found: "
                                                                + attempt.getPaymentId()));

                /*
                 * 6. Determine the corresponding Payment status.
                 */
                PaymentStatus targetPaymentStatus = mapPaymentStatus(targetAttemptStatus);

                /*
                 * 7. Validate the parent Payment transition.
                 */
                validatePaymentTransition(
                                payment.getStatus(),
                                targetPaymentStatus);

                Instant now = Instant.now();

                /*
                 * 8. Update PaymentAttempt.
                 */
                attempt.setStatus(targetAttemptStatus);

                if (targetAttemptStatus == PaymentAttemptStatus.FAILED) {
                        attempt.setFailureReason(webhook.failureReason());
                }

                attempt.setUpdatedAt(now);

                paymentAttemptRepository.save(attempt);

                /*
                 * 9. Update parent Payment.
                 */
                payment.setStatus(targetPaymentStatus);
                payment.setUpdatedAt(now);

                paymentRepository.save(payment);

                /*
                 * 10. Record provider event.
                 *
                 * This is committed in the same DB transaction as the
                 * Payment and PaymentAttempt changes.
                 */
                PaymentProviderEvent providerEvent = new PaymentProviderEvent(
                                webhook.eventId(),
                                webhook.providerPaymentId(),
                                webhook.status(),
                                now);

                providerEvent.setProcessedAt(now);

                paymentProviderEventRepository.save(providerEvent);
        }

        private PaymentAttemptStatus mapAttemptStatus(
                        String providerStatus) {

                return switch (providerStatus.toUpperCase()) {

                        case "SUCCESS" ->
                                PaymentAttemptStatus.SUCCESS;

                        case "FAILED" ->
                                PaymentAttemptStatus.FAILED;

                        default ->
                                throw new IllegalArgumentException(
                                                "Unsupported provider status: "
                                                                + providerStatus);
                };
        }

        private PaymentStatus mapPaymentStatus(
                        PaymentAttemptStatus attemptStatus) {

                return switch (attemptStatus) {

                        case SUCCESS ->
                                PaymentStatus.SUCCESS;

                        case FAILED ->
                                PaymentStatus.FAILED;

                        case CREATED, PROCESSING ->
                                throw new IllegalArgumentException(
                                                "Cannot map attempt status "
                                                                + attemptStatus
                                                                + " to final payment status");
                };
        }

        private void validateAttemptTransition(
                        PaymentAttemptStatus current,
                        PaymentAttemptStatus target) {

                boolean valid = switch (current) {

                        case CREATED ->
                                target == PaymentAttemptStatus.PROCESSING;

                        case PROCESSING ->
                                target == PaymentAttemptStatus.SUCCESS
                                                || target == PaymentAttemptStatus.FAILED;

                        case SUCCESS, FAILED ->
                                false;
                };

                if (!valid) {
                        throw new InvalidPaymentStateException(
                                        "Invalid payment attempt transition: "
                                                        + current
                                                        + " -> "
                                                        + target);
                }
        }

        private void validatePaymentTransition(
                        PaymentStatus current,
                        PaymentStatus target) {

                boolean valid = switch (current) {

                        case PENDING ->
                                target == PaymentStatus.SUCCESS
                                                || target == PaymentStatus.FAILED;

                        case SUCCESS, FAILED, REFUNDED ->
                                false;
                };

                if (!valid) {
                        throw new InvalidPaymentStateException(
                                        "Invalid payment transition: "
                                                        + current
                                                        + " -> "
                                                        + target);
                }
        }
}