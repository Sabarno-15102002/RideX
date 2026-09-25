CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,

    payment_id UUID NOT NULL,

    attempt_number INTEGER NOT NULL,

    idempotency_key VARCHAR(255) NOT NULL UNIQUE,

    provider_payment_id VARCHAR(255),

    status VARCHAR(30) NOT NULL,

    failure_reason VARCHAR(500),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_payment_attempts_payment_id
    ON payment_attempts(payment_id);

CREATE UNIQUE INDEX uk_payment_attempts_payment_number
    ON payment_attempts(payment_id, attempt_number);

CREATE UNIQUE INDEX uk_payment_attempts_provider_payment_id
    ON payment_attempts(provider_payment_id)
    WHERE provider_payment_id IS NOT NULL;