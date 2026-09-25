CREATE TABLE payments (
    id UUID PRIMARY KEY,

    trip_id UUID NOT NULL UNIQUE,

    rider_id UUID NOT NULL,

    amount NUMERIC(12, 2) NOT NULL,

    currency VARCHAR(3) NOT NULL,

    status VARCHAR(30) NOT NULL,

    payment_method VARCHAR(30),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_payments_rider_id
    ON payments(rider_id);

CREATE INDEX idx_payments_status
    ON payments(status);