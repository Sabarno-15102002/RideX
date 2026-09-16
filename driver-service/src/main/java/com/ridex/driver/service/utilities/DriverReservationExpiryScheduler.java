package com.ridex.driver.service.utilities;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ridex.driver.service.DriverReservationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DriverReservationExpiryScheduler {

    private final DriverReservationService reservationService;

    @Scheduled(fixedDelayString = "${ridex.driver.reservation-expiry-interval-ms:5000}")
    public void expireReservations() {
        reservationService.expireReservations();
    }
}