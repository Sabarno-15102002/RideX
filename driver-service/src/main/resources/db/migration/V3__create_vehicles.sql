CREATE TABLE vehicles (
    id UUID PRIMARY KEY,

    driver_id UUID NOT NULL,

    registration_number VARCHAR(30) NOT NULL UNIQUE,

    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    color VARCHAR(50) NOT NULL,

    vehicle_type VARCHAR(30) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_vehicles_driver_id
    ON vehicles(driver_id);

CREATE INDEX idx_vehicles_type
    ON vehicles(vehicle_type);