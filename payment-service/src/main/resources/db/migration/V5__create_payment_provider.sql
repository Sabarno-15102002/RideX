CREATE TABLE payment_provider_events (
    event_id VARCHAR(255) PRIMARY KEY,
    provider_payment_id VARCHAR(255),
    event_type VARCHAR(100) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_payment_provider_events_payment_id
    ON payment_provider_events(provider_payment_id);