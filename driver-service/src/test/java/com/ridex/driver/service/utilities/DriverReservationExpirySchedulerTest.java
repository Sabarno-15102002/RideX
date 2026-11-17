package com.ridex.driver.service.utilities;

import static org.mockito.Mockito.*;

import com.ridex.driver.service.DriverReservationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverReservationExpirySchedulerTest {

    @Mock
    private DriverReservationService reservationService;

    @InjectMocks
    private DriverReservationExpiryScheduler scheduler;

    @Test
    void expireReservations_shouldDelegateToReservationService() {
        scheduler.expireReservations();

        verify(reservationService).expireReservations();
    }
}
