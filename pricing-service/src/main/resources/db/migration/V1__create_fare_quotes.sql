CREATE TABLE fare_quotes (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL UNIQUE,
    rider_id UUID NOT NULL,

    base_fare NUMERIC(12, 2) NOT NULL,
    distance_km NUMERIC(10, 2) NOT NULL,
    duration_minutes NUMERIC(10, 2) NOT NULL,

    distance_fare NUMERIC(12, 2) NOT NULL,
    duration_fare NUMERIC(12, 2) NOT NULL,

    total_fare NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_fare_quotes_rider_id
    ON fare_quotes(rider_id);