CREATE TABLE driver_reservations (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL,
    trip_id UUID NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_driver_reservations_driver_id
    ON driver_reservations(driver_id);

CREATE INDEX idx_driver_reservations_expires_at
    ON driver_reservations(expires_at);

CREATE UNIQUE INDEX uk_active_driver_reservation
    ON driver_reservations(driver_id)
    WHERE status = 'ACTIVE';