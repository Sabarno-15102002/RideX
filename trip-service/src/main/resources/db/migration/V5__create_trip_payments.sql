CREATE TABLE trip_payments (
    trip_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL UNIQUE,
    rider_id UUID NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE,
    refunded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_trip_payments_payment_id
    ON trip_payments(payment_id);

CREATE INDEX idx_trip_payments_rider_id
    ON trip_payments(rider_id);

CREATE INDEX idx_trip_payments_status
    ON trip_payments(status);