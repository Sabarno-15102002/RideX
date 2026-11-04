ALTER TABLE driver_reservations
    DROP CONSTRAINT IF EXISTS driver_reservations_trip_id_key;

DROP INDEX IF EXISTS uk_driver_reservations_trip_id;