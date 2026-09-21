package com.ridex.driver.service;

import java.util.UUID;

import com.ridex.driver.entity.DriverReservation;

public interface DriverReservationService {

    boolean reserveDriver(UUID driverId, UUID tripId);

    void acceptReservation(UUID driverId, UUID tripId);

    void releaseReservation(UUID driverId, UUID tripId);

    void expireReservations();

    DriverReservation findDriverReservation(UUID tripId);

}
