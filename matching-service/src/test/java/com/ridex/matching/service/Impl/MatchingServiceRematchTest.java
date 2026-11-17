
package com.ridex.matching.service.Impl;

import com.ridex.matching.client.DriverServiceClient;
import com.ridex.matching.client.LocationServiceClient;
import com.ridex.matching.config.MatchingMetrics;
import com.ridex.matching.dto.response.NearbyDriverResponse;
import com.ridex.matching.event.DriverMatchRequestedEvent;
import com.ridex.matching.event.TripRematchingEvent;
import com.ridex.matching.repository.ProcessedEventRepository;
import com.ridex.matching.service.util.OutboxEventService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceRematchTest {

        @Mock
        private LocationServiceClient locationServiceClient;

        @Mock
        private DriverServiceClient driverServiceClient;

        @Mock
        private ProcessedEventRepository processedEventRepository;

        @Mock
        private OutboxEventService outboxEventService;

        @Mock
        private MatchingMetrics matchingMetrics;

        @InjectMocks
        private MatchingServiceImpl matchingService;

        private UUID eventId;
        private UUID tripId;
        private UUID riderId;
        private UUID rejectedDriverId;
        private UUID replacementDriverId;
        private TripRematchingEvent rematchingEvent;

        @BeforeEach
        void setUp() {
                eventId = UUID.randomUUID();
                tripId = UUID.randomUUID();
                riderId = UUID.randomUUID();
                rejectedDriverId = UUID.randomUUID();
                replacementDriverId = UUID.randomUUID();

                rematchingEvent = new TripRematchingEvent(
                                eventId,
                                tripId,
                                riderId,
                                rejectedDriverId,
                                12.9716,
                                77.5946,
                                12.9352,
                                77.6245,
                                Instant.now());
        }

        @Test
        void rematch_shouldReturnWhenNoNearbyDriversFound() {
                when(locationServiceClient.findNearbyDrivers(
                                12.9716, 77.5946, 5.0)).thenReturn(List.of());

                matchingService.rematch(rematchingEvent);

                verify(locationServiceClient).findNearbyDrivers(
                                12.9716, 77.5946, 5.0);

                verifyNoInteractions(driverServiceClient);
                verifyNoInteractions(outboxEventService);
                verifyNoInteractions(matchingMetrics);
                verifyNoInteractions(processedEventRepository);
        }

        @Test
        void rematch_shouldReserveDriverAndPublishMatchEvent() {
                NearbyDriverResponse candidate = new NearbyDriverResponse(replacementDriverId, 1.2);

                when(locationServiceClient.findNearbyDrivers(
                                anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(candidate));

                when(driverServiceClient.reserveDriver(replacementDriverId, tripId))
                                .thenReturn(true);

                matchingService.rematch(rematchingEvent);

                verify(driverServiceClient)
                                .reserveDriver(replacementDriverId, tripId);

                ArgumentCaptor<DriverMatchRequestedEvent> eventCaptor = ArgumentCaptor
                                .forClass(DriverMatchRequestedEvent.class);

                verify(outboxEventService)
                                .saveDriverMatchRequestedEvent(eventCaptor.capture());

                DriverMatchRequestedEvent savedEvent = eventCaptor.getValue();

                assertThat(savedEvent.eventId()).isNotNull();
                assertThat(savedEvent.tripId()).isEqualTo(tripId);
                assertThat(savedEvent.driverId()).isEqualTo(replacementDriverId);
                assertThat(savedEvent.requestedAt()).isNotNull();

                verify(matchingMetrics).recordMatchingAttempt();

                verify(matchingMetrics, never()).recordMatchingSuccess();
                verify(matchingMetrics, never()).recordMatchingFailure();
                verify(matchingMetrics, never()).recordMatchingDuration(anyLong());
        }

        @Test
        void rematch_shouldTryNextDriverWhenFirstReservationFails() {
                UUID secondDriverId = UUID.randomUUID();

                NearbyDriverResponse firstCandidate = new NearbyDriverResponse(replacementDriverId, 1.0);

                NearbyDriverResponse secondCandidate = new NearbyDriverResponse(secondDriverId, 2.0);

                when(locationServiceClient.findNearbyDrivers(
                                anyDouble(), anyDouble(), anyDouble()))
                                .thenReturn(List.of(firstCandidate, secondCandidate));

                when(driverServiceClient.reserveDriver(replacementDriverId, tripId))
                                .thenReturn(false);

                when(driverServiceClient.reserveDriver(secondDriverId, tripId))
                                .thenReturn(true);

                matchingService.rematch(rematchingEvent);

                verify(driverServiceClient)
                                .reserveDriver(replacementDriverId, tripId);

                verify(driverServiceClient)
                                .reserveDriver(secondDriverId, tripId);

                ArgumentCaptor<DriverMatchRequestedEvent> eventCaptor = ArgumentCaptor
                                .forClass(DriverMatchRequestedEvent.class);

                verify(outboxEventService)
                                .saveDriverMatchRequestedEvent(eventCaptor.capture());

                assertThat(eventCaptor.getValue().driverId())
                                .isEqualTo(secondDriverId);

                verify(matchingMetrics).recordMatchingAttempt();
                verify(matchingMetrics, never()).recordMatchingSuccess();
                verify(matchingMetrics, never()).recordMatchingFailure();
        }

        @Test
        void rematch_shouldNotPublishEventWhenAllReservationsFail() {
                NearbyDriverResponse firstCandidate = new NearbyDriverResponse(replacementDriverId, 1.0);

                NearbyDriverResponse secondCandidate = new NearbyDriverResponse(UUID.randomUUID(), 2.0);

                when(locationServiceClient.findNearbyDrivers(
                                anyDouble(), anyDouble(), anyDouble()))
                                .thenReturn(List.of(firstCandidate, secondCandidate));

                when(driverServiceClient.reserveDriver(any(UUID.class), eq(tripId)))
                                .thenReturn(false);

                matchingService.rematch(rematchingEvent);

                verify(driverServiceClient, times(2))
                                .reserveDriver(any(UUID.class), eq(tripId));

                verifyNoInteractions(outboxEventService);
                verifyNoInteractions(matchingMetrics);
        }

        @Test
        void rematch_shouldPropagateLocationServiceException() {
                when(locationServiceClient.findNearbyDrivers(
                                anyDouble(), anyDouble(), anyDouble()))
                                .thenThrow(new IllegalStateException("Location service unavailable"));

                assertThrows(
                                IllegalStateException.class,
                                () -> matchingService.rematch(rematchingEvent));

                verifyNoInteractions(driverServiceClient);
                verifyNoInteractions(outboxEventService);
                verifyNoInteractions(matchingMetrics);
                verifyNoInteractions(processedEventRepository);
        }

        @Test
        void rematch_shouldPropagateDriverServiceException() {
                NearbyDriverResponse candidate = new NearbyDriverResponse(replacementDriverId, 1.0);

                when(locationServiceClient.findNearbyDrivers(
                                anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(candidate));

                when(driverServiceClient.reserveDriver(replacementDriverId, tripId))
                                .thenThrow(new IllegalStateException("Driver service unavailable"));

                assertThrows(
                                IllegalStateException.class,
                                () -> matchingService.rematch(rematchingEvent));

                verify(outboxEventService, never())
                                .saveDriverMatchRequestedEvent(any());

                verify(matchingMetrics, never()).recordMatchingAttempt();
        }
}
