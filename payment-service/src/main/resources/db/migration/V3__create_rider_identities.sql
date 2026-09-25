CREATE TABLE rider_identities (
    user_id UUID PRIMARY KEY,
    rider_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_rider_identities_rider_id
    ON rider_identities(rider_id);