package com.ridex.matching.service.Impl;

import com.ridex.matching.client.DriverServiceClient;
import com.ridex.matching.client.LocationServiceClient;
import com.ridex.matching.config.MatchingMetrics;
import com.ridex.matching.dto.response.NearbyDriverResponse;
import com.ridex.matching.entity.ProcessedEvent;
import com.ridex.matching.event.DriverMatchRequestedEvent;
import com.ridex.matching.event.TripRequestedEvent;
import com.ridex.matching.repository.ProcessedEventRepository;
import com.ridex.matching.service.util.OutboxEventService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
class MatchingServiceImplTest {

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
        private UUID driverId;
        private TripRequestedEvent tripEvent;

        @BeforeEach
        void setUp() {
                eventId = UUID.randomUUID();
                tripId = UUID.randomUUID();
                riderId = UUID.randomUUID();
                driverId = UUID.randomUUID();

                tripEvent = new TripRequestedEvent(
                                eventId,
                                tripId,
                                riderId,
                                12.9716,
                                77.5946,
                                12.9352,
                                77.6245,
                                Instant.now());
        }

        @Test
        void matchTrip_shouldReturnWhenNoNearbyDriversFound() {
                when(locationServiceClient.findNearbyDrivers(
                                12.9716, 77.5946, 5.0)).thenReturn(List.of());

                matchingService.matchTrip(tripEvent);

                verify(locationServiceClient).findNearbyDrivers(
                                12.9716, 77.5946, 5.0);
                verifyNoInteractions(driverServiceClient);
                verifyNoInteractions(outboxEventService);
                verifyNoInteractions(matchingMetrics);
                verifyNoInteractions(processedEventRepository);
        }

        @Test
        void matchTrip_shouldReserveFirstDriverAndPublishMatchEvent() {
                NearbyDriverResponse candidate = new NearbyDriverResponse(driverId, 1.2);

                when(locationServiceClient.findNearbyDrivers(
                                anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(candidate));

                when(driverServiceClient.reserveDriver(driverId, tripId))
                                .thenReturn(true);

                matchingService.matchTrip(tripEvent);

                verify(processedEventRepository).save(any(ProcessedEvent.class));
                verify(driverServiceClient).reserveDriver(driverId, tripId);

                ArgumentCaptor<DriverMatchRequestedEvent> eventCaptor = ArgumentCaptor
                                .forClass(DriverMatchRequestedEvent.class);

                verify(outboxEventService)
                                .saveDriverMatchRequestedEvent(eventCaptor.capture());

                DriverMatchRequestedEvent savedEvent = eventCaptor.getValue();

                assertThat(savedEvent.eventId()).isNotNull();
                assertThat(savedEvent.tripId()).isEqualTo(tripId);
                assertThat(savedEvent.driverId()).isEqualTo(driverId);
                assertThat(savedEvent.requestedAt()).isNotNull();

                verify(matchingMetrics).recordMatchingSuccess();
                verify(matchingMetrics).recordMatchingDuration(anyLong());
                verify(matchingMetrics).recordMatchingAttempt();
                verify(matchingMetrics, never()).recordMatchingFailure();
        }

        @Test
        void matchTrip_shouldTryNextDriverWhenFirstReservationFails() {
                UUID secondDriverId = UUID.randomUUID();

                NearbyDriverResponse firstCandidate = new NearbyDriverResponse(driverId, 1.0);

                NearbyDriverResponse secondCandidate = new NearbyDriverResponse(secondDriverId, 2.0);

                when(locationServiceClient.findNearbyDrivers(
                                anyDouble(), anyDouble(), anyDouble()))
                                .thenReturn(List.of(firstCandidate, secondCandidate));

                when(driverServiceClient.reserveDriver(driverId, tripId))
                                .thenReturn(false);

                when(driverServiceClient.reserveDriver(secondDriverId, tripId))
                                .thenReturn(true);

                matchingService.matchTrip(tripEvent);

                verify(driverServiceClient).reserveDriver(driverId, tripId);
                verify(driverServiceClient).reserveDriver(secondDriverId, tripId);

                ArgumentCaptor<DriverMatchRequestedEvent> eventCaptor = ArgumentCaptor
                                .forClass(DriverMatchRequestedEvent.class);

                verify(outboxEventService)
                                .saveDriverMatchRequestedEvent(eventCaptor.capture());

                assertThat(eventCaptor.getValue().driverId())
                                .isEqualTo(secondDriverId);

                verify(matchingMetrics).recordMatchingFailure();
                verify(matchingMetrics).recordMatchingSuccess();
                verify(matchingMetrics).recordMatchingDuration(anyLong());
                verify(matchingMetrics).recordMatchingAttempt();
        }

        @Test
        void matchTrip_shouldStopAfterAllDriverReservationsFail() {
                UUID secondDriverId = UUID.randomUUID();

                NearbyDriverResponse firstCandidate = new NearbyDriverResponse(driverId, 1.0);

                NearbyDriverResponse secondCandidate = new NearbyDriverResponse(secondDriverId, 2.0);

                when(locationServiceClient.findNearbyDrivers(
                                anyDouble(), anyDouble(), anyDouble()))
                                .thenReturn(List.of(firstCandidate, secondCandidate));

                when(driverServiceClient.reserveDriver(any(UUID.class), eq(tripId)))
                                .thenReturn(false);

                matchingService.matchTrip(tripEvent);

                verify(driverServiceClient, times(2))
                                .reserveDriver(any(UUID.class), eq(tripId));

                verify(outboxEventService, never())
                                .saveDriverMatchRequestedEvent(any());

                verify(matchingMetrics, times(2)).recordMatchingFailure();
                verify(matchingMetrics, never()).recordMatchingSuccess();
                verify(matchingMetrics, never()).recordMatchingDuration(anyLong());
                verify(matchingMetrics, never()).recordMatchingAttempt();
        }

        @Test
        void matchTrip_shouldSaveProcessedEventBeforeAttemptingReservation() {
                NearbyDriverResponse candidate = new NearbyDriverResponse(driverId, 1.0);

                when(locationServiceClient.findNearbyDrivers(
                                anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(candidate));

                when(driverServiceClient.reserveDriver(driverId, tripId))
                                .thenReturn(true);

                matchingService.matchTrip(tripEvent);

                InOrder inOrder = inOrder(
                                processedEventRepository,
                                driverServiceClient);

                inOrder.verify(processedEventRepository)
                                .save(any(ProcessedEvent.class));

                inOrder.verify(driverServiceClient)
                                .reserveDriver(driverId, tripId);
        }

        @Test
        void matchTrip_shouldPropagateLocationServiceException() {
                when(locationServiceClient.findNearbyDrivers(
                                anyDouble(), anyDouble(), anyDouble()))
                                .thenThrow(new IllegalStateException("Location service unavailable"));

                assertThrows(
                                IllegalStateException.class,
                                () -> matchingService.matchTrip(tripEvent));

                verifyNoInteractions(driverServiceClient);
                verifyNoInteractions(processedEventRepository);
                verifyNoInteractions(outboxEventService);
                verifyNoInteractions(matchingMetrics);
        }
}
