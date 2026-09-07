CREATE TABLE drivers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,

    name VARCHAR(150) NOT NULL,
    license_number VARCHAR(50) UNIQUE,

    rating NUMERIC(3, 2) NOT NULL DEFAULT 5.00,

    status VARCHAR(30) NOT NULL DEFAULT 'OFFLINE',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_drivers_status
    ON drivers(status);

CREATE INDEX idx_drivers_user_id
    ON drivers(user_id);