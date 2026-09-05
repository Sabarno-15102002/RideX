CREATE TABLE riders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,

    name VARCHAR(150) NOT NULL,

    rating NUMERIC(3,2) NOT NULL DEFAULT 5.00,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_riders_user_id
    ON riders(user_id);

CREATE TABLE saved_locations (
    id UUID PRIMARY KEY,

    rider_id UUID NOT NULL,

    label VARCHAR(50) NOT NULL,

    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,

    address VARCHAR(500),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_saved_location_rider
        FOREIGN KEY (rider_id)
        REFERENCES riders(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_saved_locations_rider_id
    ON saved_locations(rider_id);