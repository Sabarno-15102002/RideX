package com.ridex.driver.service;

import java.util.UUID;

public interface DriverReservationService {

    boolean reserveDriver(UUID driverId, UUID tripId);

    void acceptReservation(UUID driverId, UUID tripId);

    void releaseReservation(UUID driverId, UUID tripId);

    void expireReservations();

}
