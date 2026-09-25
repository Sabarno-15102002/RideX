ALTER TABLE payments
    ADD COLUMN provider_payment_id VARCHAR(255),
    ADD COLUMN idempotency_key VARCHAR(255) NOT NULL;

CREATE UNIQUE INDEX uk_payments_provider_payment_id
    ON payments(provider_payment_id)
    WHERE provider_payment_id IS NOT NULL;
CREATE UNIQUE INDEX uk_payments_idempotency_key
    ON payments(idempotency_key);