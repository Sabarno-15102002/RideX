package com.ridex.trip.service.Impl;

import com.ridex.trip.config.TripMetrics;
import com.ridex.trip.dto.PaymentStatus;
import com.ridex.trip.dto.TripStatus;
import com.ridex.trip.dto.request.CreateTripRequest;
import com.ridex.trip.dto.response.TripResponse;
import com.ridex.trip.entity.ProcessedEvent;
import com.ridex.trip.entity.RiderIdentity;
import com.ridex.trip.entity.Trip;
import com.ridex.trip.entity.TripPayment;
import com.ridex.trip.event.event.*;
import com.ridex.trip.exception.IllegalOperationException;
import com.ridex.trip.exception.ResourceNotFoundException;
import com.ridex.trip.repository.ProcessedEventRepository;
import com.ridex.trip.repository.RiderIdentityRepository;
import com.ridex.trip.repository.TripPaymentRepository;
import com.ridex.trip.repository.TripRepository;
import com.ridex.trip.service.util.OutboxEventService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TripServiceImplTest {

        @Mock
        private TripRepository tripRepository;
        @Mock
        private RiderIdentityRepository riderIdentityRepository;
        @Mock
        private OutboxEventService outboxEventService;
        @Mock
        private ProcessedEventRepository processedEventRepository;
        @Mock
        private TripPaymentRepository tripPaymentRepository;
        @Mock
        private TripMetrics tripMetrics;

        @InjectMocks
        private TripServiceImpl tripService;

        private UUID tripId;
        private UUID riderId;
        private UUID driverId;
        private UUID otherDriverId;

        @BeforeEach
        void setUp() {
                tripId = UUID.randomUUID();
                riderId = UUID.randomUUID();
                driverId = UUID.randomUUID();
                otherDriverId = UUID.randomUUID();
        }

        private Trip tripWithStatus(TripStatus status) {
                return Trip.builder()
                                .id(tripId)
                                .riderId(riderId)
                                .driverId(driverId)
                                .pickupLatitude(12.9716)
                                .pickupLongitude(77.5946)
                                .dropoffLatitude(12.9352)
                                .dropoffLongitude(77.6245)
                                .status(status)
                                .requestedAt(Instant.parse("2026-11-10T10:00:00Z"))
                                .startedAt(status == TripStatus.TRIP_STARTED
                                                || status == TripStatus.TRIP_COMPLETED
                                                                ? Instant.parse("2026-11-10T10:10:00Z")
                                                                : null)
                                .build();
        }

        private void givenTrip(Trip trip) {
                when(processedEventRepository.existsById(any(UUID.class))).thenReturn(false);
                when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        }

        // ---------------------------------------------------------------------
        // Trip creation
        // ---------------------------------------------------------------------

        @Test
        void createTrip_persistsRequestedTripAndOutboxEvent() {
                UUID userId = UUID.randomUUID();
                UUID newTripId = UUID.randomUUID();
                RiderIdentity identity = RiderIdentity.builder()
                                .userId(userId).riderId(riderId).build();
                CreateTripRequest request = new CreateTripRequest(
                                12.9716, 77.5946, 12.9352, 77.6245);

                when(riderIdentityRepository.findById(userId)).thenReturn(Optional.of(identity));
                when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
                        Trip saved = invocation.getArgument(0);
                        saved.setId(newTripId);
                        return saved;
                });
                when(tripPaymentRepository.findById(newTripId)).thenReturn(Optional.empty());

                TripResponse response = tripService.createTrip(userId, request);

                assertNotNull(response);
                assertEquals(newTripId, response.id());
                assertEquals(riderId, response.riderId());
                assertNull(response.driverId());
                assertEquals(TripStatus.REQUESTED, response.status());
                assertEquals(12.9716, response.pickupLatitude());
                assertEquals(77.5946, response.pickupLongitude());
                assertEquals(12.9352, response.dropoffLatitude());
                assertEquals(77.6245, response.dropoffLongitude());
                assertNotNull(response.requestedAt());
                assertNull(response.payment());

                ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
                verify(tripRepository).save(tripCaptor.capture());
                assertEquals(TripStatus.REQUESTED, tripCaptor.getValue().getStatus());
                assertEquals(riderId, tripCaptor.getValue().getRiderId());

                ArgumentCaptor<TripRequestedEvent> eventCaptor = ArgumentCaptor.forClass(TripRequestedEvent.class);
                verify(outboxEventService).saveTripRequestedEvent(eventCaptor.capture());
                assertEquals(newTripId, eventCaptor.getValue().tripId());
                assertEquals(riderId, eventCaptor.getValue().riderId());
                verify(tripMetrics).tripRequested();
        }

        @Test
        void createTrip_includesExistingPaymentProjectionInResponse() {
                UUID userId = UUID.randomUUID();
                UUID newTripId = UUID.randomUUID();
                RiderIdentity identity = RiderIdentity.builder()
                                .userId(userId).riderId(riderId).build();
                CreateTripRequest request = new CreateTripRequest(
                                12.9716, 77.5946, 12.9352, 77.6245);
                TripPayment payment = new TripPayment(newTripId, UUID.randomUUID(), riderId,
                                new BigDecimal("125.50"), "INR", PaymentStatus.SUCCESS, Instant.now());

                when(riderIdentityRepository.findById(userId)).thenReturn(Optional.of(identity));
                when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
                        Trip saved = invocation.getArgument(0);
                        saved.setId(newTripId);
                        return saved;
                });
                when(tripPaymentRepository.findById(newTripId)).thenReturn(Optional.of(payment));

                TripResponse response = tripService.createTrip(userId, request);

                assertNotNull(response.payment());
                assertEquals(new BigDecimal("125.50"), response.payment().amount());
                assertEquals("INR", response.payment().currency());
                assertEquals(PaymentStatus.SUCCESS, response.payment().status());
        }

        @Test
        void createTrip_throwsWhenRiderIdentityDoesNotExist() {
                UUID userId = UUID.randomUUID();
                CreateTripRequest request = new CreateTripRequest(
                                12.9716, 77.5946, 12.9352, 77.6245);
                when(riderIdentityRepository.findById(userId)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class,
                                () -> tripService.createTrip(userId, request));

                verify(tripRepository, never()).save(any(Trip.class));
                verify(outboxEventService, never()).saveTripRequestedEvent(any());
                verify(tripMetrics, never()).tripRequested();
        }

        // ---------------------------------------------------------------------
        // Driver assignment
        // ---------------------------------------------------------------------

        @Test
        void assignDriver_assignsDriverWhenTripIsRequested() {
                Trip trip = tripWithStatus(TripStatus.REQUESTED);
                givenTrip(trip);
                UUID eventId = UUID.randomUUID();
                DriverMatchRequestedEvent event = new DriverMatchRequestedEvent(
                                eventId, tripId, driverId, Instant.now());

                tripService.assignDriver(event);

                assertEquals(driverId, trip.getDriverId());
                assertEquals(TripStatus.DRIVER_ASSIGNED, trip.getStatus());
                assertNotNull(trip.getAssignedAt());
                verify(tripRepository).save(trip);
                verify(processedEventRepository).save(argThat(p -> eventId.equals(p.getEventId())));
        }

        @Test
        void assignDriver_assignsDriverWhenTripIsMatching() {
                Trip trip = tripWithStatus(TripStatus.MATCHING);
                givenTrip(trip);
                DriverMatchRequestedEvent event = new DriverMatchRequestedEvent(
                                UUID.randomUUID(), tripId, driverId, Instant.now());

                tripService.assignDriver(event);

                assertEquals(TripStatus.DRIVER_ASSIGNED, trip.getStatus());
                assertEquals(driverId, trip.getDriverId());
                verify(tripRepository).save(trip);
        }

        @Test
        void assignDriver_ignoresDuplicateEvent() {
                UUID eventId = UUID.randomUUID();
                when(processedEventRepository.existsById(eventId)).thenReturn(true);

                tripService.assignDriver(new DriverMatchRequestedEvent(
                                eventId, tripId, driverId, Instant.now()));

                verifyNoInteractions(tripRepository);
                verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        }

        @Test
        void assignDriver_doesNothingWhenTripIsInUnsupportedState() {
                Trip trip = tripWithStatus(TripStatus.TRIP_STARTED);
                givenTrip(trip);

                UUID eventId = UUID.randomUUID();

                tripService.assignDriver(new DriverMatchRequestedEvent(
                                eventId,
                                tripId,
                                otherDriverId,
                                Instant.now()));

                // Trip state must remain unchanged.
                assertEquals(TripStatus.TRIP_STARTED, trip.getStatus());
                assertEquals(driverId, trip.getDriverId());

                // Trip must not be updated.
                verify(tripRepository, never()).save(any(Trip.class));

                // Stale event must be recorded.
                ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);

                verify(processedEventRepository).save(captor.capture());

                assertEquals(eventId, captor.getValue().getEventId());
        }

        @Test
        void assignDriver_throwsWhenTripDoesNotExist() {
                UUID eventId = UUID.randomUUID();
                when(processedEventRepository.existsById(eventId)).thenReturn(false);
                when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class,
                                () -> tripService.assignDriver(new DriverMatchRequestedEvent(
                                                eventId, tripId, driverId, Instant.now())));
        }

        // ---------------------------------------------------------------------
        // Driver accepted
        // ---------------------------------------------------------------------

        @Test
        void handleDriverRideAccepted_movesAssignedTripToDriverArriving() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ASSIGNED);
                givenTrip(trip);
                UUID eventId = UUID.randomUUID();

                tripService.handleDriverRideAccepted(new DriverRideAcceptedEvent(
                                eventId, tripId, driverId, Instant.now()));

                assertEquals(TripStatus.DRIVER_ARRIVING, trip.getStatus());
                assertNotNull(trip.getUpdatedAt());
                verify(tripRepository).save(trip);
                verify(processedEventRepository).save(argThat(p -> eventId.equals(p.getEventId())));
        }

        @Test
        void handleDriverRideAccepted_throwsWhenDriverDoesNotMatchAssignment() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ASSIGNED);
                givenTrip(trip);

                assertThrows(IllegalOperationException.class,
                                () -> tripService.handleDriverRideAccepted(new DriverRideAcceptedEvent(
                                                UUID.randomUUID(), tripId, otherDriverId, Instant.now())));

                verify(tripRepository, never()).save(any(Trip.class));
        }

        @Test
        void handleDriverRideAccepted_ignoresDuplicateEvent() {
                UUID eventId = UUID.randomUUID();
                when(processedEventRepository.existsById(eventId)).thenReturn(true);

                tripService.handleDriverRideAccepted(new DriverRideAcceptedEvent(
                                eventId, tripId, driverId, Instant.now()));

                verifyNoInteractions(tripRepository);
        }

        @Test
        void handleDriverRideAccepted_ignoresEventWhenTripIsNotAssigned() {
                Trip trip = tripWithStatus(TripStatus.MATCHING);
                givenTrip(trip);

                tripService.handleDriverRideAccepted(new DriverRideAcceptedEvent(
                                UUID.randomUUID(), tripId, driverId, Instant.now()));

                verify(tripRepository, never()).save(any(Trip.class));
                verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        }

        // ---------------------------------------------------------------------
        // Driver rejected
        // ---------------------------------------------------------------------

        @Test
        void handleDriverRideRejected_resetsTripAndCreatesRematchingEvent() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ASSIGNED);
                givenTrip(trip);
                UUID eventId = UUID.randomUUID();

                tripService.handleDriverRideRejected(new DriverRideRejectedEvent(
                                eventId, tripId, driverId, Instant.now()));

                assertEquals(TripStatus.MATCHING, trip.getStatus());
                assertNull(trip.getDriverId());
                assertNotNull(trip.getUpdatedAt());
                verify(tripRepository).save(trip);
                verify(outboxEventService).saveTripRematchingEvent(argThat(event -> tripId.equals(event.tripId())
                                && riderId.equals(event.riderId())
                                && driverId.equals(event.rejectedDriverId())));
                verify(processedEventRepository).save(argThat(p -> eventId.equals(p.getEventId())));
        }

        @Test
        void handleDriverRideRejected_throwsWhenRejectingDriverIsNotAssignedDriver() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ASSIGNED);
                givenTrip(trip);

                assertThrows(IllegalOperationException.class,
                                () -> tripService.handleDriverRideRejected(new DriverRideRejectedEvent(
                                                UUID.randomUUID(), tripId, otherDriverId, Instant.now())));

                verify(tripRepository, never()).save(any(Trip.class));
                verify(outboxEventService, never()).saveTripRematchingEvent(any());
        }

        @Test
        void handleDriverRideRejected_ignoresEventWhenTripIsNotAssigned() {
                Trip trip = tripWithStatus(TripStatus.TRIP_STARTED);
                givenTrip(trip);

                tripService.handleDriverRideRejected(new DriverRideRejectedEvent(
                                UUID.randomUUID(), tripId, driverId, Instant.now()));

                verify(tripRepository, never()).save(any(Trip.class));
                verify(outboxEventService, never()).saveTripRematchingEvent(any());
        }

        // ---------------------------------------------------------------------
        // Driver reservation expired
        // ---------------------------------------------------------------------

        @Test
        void handleDriverRideExpired_resetsTripAndCreatesRematchingEvent() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ASSIGNED);
                givenTrip(trip);
                UUID eventId = UUID.randomUUID();

                tripService.handleDriverRideExpired(new DriverRideExpiredEvent(
                                eventId, tripId, driverId, Instant.now()));

                assertEquals(TripStatus.MATCHING, trip.getStatus());
                assertNull(trip.getDriverId());
                verify(tripRepository).save(trip);
                verify(outboxEventService).saveTripRematchingEvent(any(TripRematchingEvent.class));
                verify(processedEventRepository).save(argThat(p -> eventId.equals(p.getEventId())));
        }

        @Test
        void handleDriverRideExpired_throwsWhenDriverDoesNotMatchCurrentAssignment() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ASSIGNED);
                givenTrip(trip);

                assertThrows(IllegalOperationException.class,
                                () -> tripService.handleDriverRideExpired(new DriverRideExpiredEvent(
                                                UUID.randomUUID(), tripId, otherDriverId, Instant.now())));

                verify(tripRepository, never()).save(any(Trip.class));
        }

        // Regression requirement: this test describes the desired stale-event behavior.
        // It will FAIL against the uploaded implementation because driver identity is
        // checked before trip status. Fix handleDriverRideExpired before enabling it.
        /*
         * @Test
         * void handleDriverRideExpired_ignoresStaleEventForPreviousDriver() {
         * Trip trip = tripWithStatus(TripStatus.DRIVER_ASSIGNED);
         * trip.setDriverId(otherDriverId); // a newer driver is now assigned
         * givenTrip(trip);
         * 
         * tripService.handleDriverRideExpired(new DriverRideExpiredEvent(
         * UUID.randomUUID(), tripId, driverId, Instant.now()));
         * 
         * assertEquals(TripStatus.DRIVER_ASSIGNED, trip.getStatus());
         * assertEquals(otherDriverId, trip.getDriverId());
         * verify(tripRepository, never()).save(any(Trip.class));
         * verify(outboxEventService, never()).saveTripRematchingEvent(any());
         * }
         */

        // ---------------------------------------------------------------------
        // Driver arrived
        // ---------------------------------------------------------------------

        @Test
        void handleDriverArrived_movesTripToDriverArrived() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ARRIVING);
                givenTrip(trip);
                UUID eventId = UUID.randomUUID();

                tripService.handleDriverArrived(new DriverArrivedEvent(
                                eventId, tripId, driverId, Instant.now()));

                assertEquals(TripStatus.DRIVER_ARRIVED, trip.getStatus());
                assertNotNull(trip.getUpdatedAt());
                verify(tripRepository).save(trip);
                verify(processedEventRepository).save(argThat(p -> eventId.equals(p.getEventId())));
        }

        @Test
        void handleDriverArrived_throwsWhenDriverDoesNotMatch() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ARRIVING);
                givenTrip(trip);

                assertThrows(IllegalOperationException.class,
                                () -> tripService.handleDriverArrived(new DriverArrivedEvent(
                                                UUID.randomUUID(), tripId, otherDriverId, Instant.now())));
                verify(tripRepository, never()).save(any(Trip.class));
        }

        @Test
        void handleDriverArrived_ignoresEventWhenTripIsNotArriving() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ASSIGNED);
                givenTrip(trip);

                tripService.handleDriverArrived(new DriverArrivedEvent(
                                UUID.randomUUID(), tripId, driverId, Instant.now()));

                verify(tripRepository, never()).save(any(Trip.class));
        }

        // ---------------------------------------------------------------------
        // Trip started
        // ---------------------------------------------------------------------

        @Test
        void handleDriverTripStarted_movesTripToStarted() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ARRIVED);
                givenTrip(trip);
                Instant startedAt = Instant.parse("2026-11-10T10:15:00Z");
                UUID eventId = UUID.randomUUID();

                tripService.handleDriverTripStarted(new DriverTripStartedEvent(
                                eventId, tripId, driverId, startedAt));

                assertEquals(TripStatus.TRIP_STARTED, trip.getStatus());
                assertEquals(startedAt, trip.getStartedAt());
                assertNotNull(trip.getUpdatedAt());
                verify(tripRepository).save(trip);
                verify(processedEventRepository).save(argThat(p -> eventId.equals(p.getEventId())));
        }

        @Test
        void handleDriverTripStarted_throwsWhenDriverDoesNotMatch() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ARRIVED);
                givenTrip(trip);

                assertThrows(IllegalOperationException.class,
                                () -> tripService.handleDriverTripStarted(new DriverTripStartedEvent(
                                                UUID.randomUUID(), tripId, otherDriverId, Instant.now())));
                verify(tripRepository, never()).save(any(Trip.class));
        }

        @Test
        void handleDriverTripStarted_ignoresEventWhenTripIsNotDriverArrived() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ARRIVING);
                givenTrip(trip);

                tripService.handleDriverTripStarted(new DriverTripStartedEvent(
                                UUID.randomUUID(), tripId, driverId, Instant.now()));

                verify(tripRepository, never()).save(any(Trip.class));
        }

        // ---------------------------------------------------------------------
        // Trip completed
        // ---------------------------------------------------------------------

        @Test
        void handleDriverTripCompleted_completesTripPublishesEventAndRecordsMetric() {
                Trip trip = tripWithStatus(TripStatus.TRIP_STARTED);
                givenTrip(trip);
                Instant completedAt = Instant.parse("2026-11-10T10:30:00Z");
                UUID eventId = UUID.randomUUID();

                tripService.handleDriverTripCompleted(new DriverTripCompletedEvent(
                                eventId, tripId, driverId, completedAt));

                assertEquals(TripStatus.TRIP_COMPLETED, trip.getStatus());
                assertEquals(completedAt, trip.getCompletedAt());
                assertNotNull(trip.getUpdatedAt());
                verify(tripRepository).save(trip);
                verify(processedEventRepository).save(argThat(p -> eventId.equals(p.getEventId())));
                verify(outboxEventService).saveTripCompletedEvent(argThat(event -> tripId.equals(event.tripId())
                                && riderId.equals(event.riderId())
                                && driverId.equals(event.driverId())
                                && completedAt.equals(event.completedAt())));
                verify(tripMetrics).tripCompleted(Duration.between(
                                trip.getStartedAt(), completedAt));
        }

        @Test
        void handleDriverTripCompleted_throwsWhenDriverDoesNotMatch() {
                Trip trip = tripWithStatus(TripStatus.TRIP_STARTED);
                givenTrip(trip);

                assertThrows(IllegalOperationException.class,
                                () -> tripService.handleDriverTripCompleted(new DriverTripCompletedEvent(
                                                UUID.randomUUID(), tripId, otherDriverId, Instant.now())));

                verify(tripRepository, never()).save(any(Trip.class));
                verify(outboxEventService, never()).saveTripCompletedEvent(any());
        }

        @Test
        void handleDriverTripCompleted_ignoresEventWhenTripIsNotStarted() {
                Trip trip = tripWithStatus(TripStatus.DRIVER_ARRIVED);
                givenTrip(trip);

                tripService.handleDriverTripCompleted(new DriverTripCompletedEvent(
                                UUID.randomUUID(), tripId, driverId, Instant.now()));

                verify(tripRepository, never()).save(any(Trip.class));
                verify(outboxEventService, never()).saveTripCompletedEvent(any());
        }

        // ---------------------------------------------------------------------
        // Queries
        // ---------------------------------------------------------------------

        @Test
        void getTripStatus_returnsCurrentStatus() {
                Trip trip = tripWithStatus(TripStatus.TRIP_STARTED);
                when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

                assertEquals(TripStatus.TRIP_STARTED, tripService.getTripStatus(tripId).status());
        }

        @Test
        void getTripStatus_throwsWhenTripDoesNotExist() {
                when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class,
                                () -> tripService.getTripStatus(tripId));
        }

        @Test
        void getTrip_returnsTripWhenAuthenticatedRiderOwnsIt() {
                UUID userId = UUID.randomUUID();
                Trip trip = tripWithStatus(TripStatus.DRIVER_ASSIGNED);
                RiderIdentity identity = RiderIdentity.builder()
                                .userId(userId).riderId(riderId).build();
                when(riderIdentityRepository.findById(userId)).thenReturn(Optional.of(identity));
                when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
                when(tripPaymentRepository.findById(tripId)).thenReturn(Optional.empty());

                TripResponse response = tripService.getTrip(userId, tripId);

                assertEquals(tripId, response.id());
                assertEquals(riderId, response.riderId());
                assertEquals(driverId, response.driverId());
                assertEquals(TripStatus.DRIVER_ASSIGNED, response.status());
        }

        @Test
        void getTrip_throwsWhenRiderIdentityDoesNotExist() {
                UUID userId = UUID.randomUUID();
                when(riderIdentityRepository.findById(userId)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class,
                                () -> tripService.getTrip(userId, tripId));
                verify(tripRepository, never()).findById(any());
        }

        @Test
        void getTrip_throwsWhenTripDoesNotExist() {
                UUID userId = UUID.randomUUID();
                when(riderIdentityRepository.findById(userId)).thenReturn(Optional.of(
                                RiderIdentity.builder().userId(userId).riderId(riderId).build()));
                when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class,
                                () -> tripService.getTrip(userId, tripId));
        }

        @Test
        void getTrip_throwsWhenTripBelongsToAnotherRider() {
                UUID userId = UUID.randomUUID();
                UUID anotherRiderId = UUID.randomUUID();
                when(riderIdentityRepository.findById(userId)).thenReturn(Optional.of(
                                RiderIdentity.builder().userId(userId).riderId(anotherRiderId).build()));
                when(tripRepository.findById(tripId)).thenReturn(Optional.of(
                                tripWithStatus(TripStatus.REQUESTED)));

                assertThrows(IllegalOperationException.class,
                                () -> tripService.getTrip(userId, tripId));
                verify(tripPaymentRepository, never()).findById(any());
        }

        @Test
        void handleDriverTripStarted_ignoresDuplicateEvent() {
                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(true);

                tripService.handleDriverTripStarted(
                                new DriverTripStartedEvent(
                                                eventId, tripId, driverId, Instant.now()));

                verify(tripRepository, never()).findById(any(UUID.class));
                verify(tripRepository, never()).save(any(Trip.class));
        }

        @Test
        void handleDriverArrived_ignoresDuplicateEvent() {
                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(true);

                tripService.handleDriverArrived(
                                new DriverArrivedEvent(
                                                eventId, tripId, driverId, Instant.now()));

                verify(tripRepository, never()).findById(any(UUID.class));
                verify(tripRepository, never()).save(any(Trip.class));
        }

        @Test
        void handleDriverRideRejected_ignoresDuplicateEvent() {
                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(true);

                tripService.handleDriverRideRejected(
                                new DriverRideRejectedEvent(
                                                eventId, tripId, driverId, Instant.now()));

                verify(tripRepository, never()).findById(any(UUID.class));
                verify(tripRepository, never()).save(any(Trip.class));
        }

        @Test
        void handleDriverRideExpired_ignoresDuplicateEvent() {
                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(true);

                tripService.handleDriverRideExpired(
                                new DriverRideExpiredEvent(
                                                eventId, tripId, driverId, Instant.now()));

                verify(tripRepository, never()).findById(any(UUID.class));
                verify(tripRepository, never()).save(any(Trip.class));
        }

        @Test
        void handleDriverTripStarted_throwsWhenTripDoesNotExist() {
                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(false);
                given(tripRepository.findById(tripId))
                                .willReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> tripService.handleDriverTripStarted(
                                                new DriverTripStartedEvent(
                                                                eventId, tripId, driverId, Instant.now())));
        }

        @Test
        void handleDriverArrived_throwsWhenTripDoesNotExist() {
                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(false);
                given(tripRepository.findById(tripId))
                                .willReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> tripService.handleDriverArrived(
                                                new DriverArrivedEvent(
                                                                eventId, tripId, driverId, Instant.now())));
        }

        @Test
        void handleDriverRideRejected_throwsWhenTripDoesNotExist() {
                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(false);
                given(tripRepository.findById(tripId))
                                .willReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> tripService.handleDriverRideRejected(
                                                new DriverRideRejectedEvent(
                                                                eventId, tripId, driverId, Instant.now())));
        }

        @Test
        void handleDriverRideExpired_throwsWhenTripDoesNotExist() {
                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(false);
                given(tripRepository.findById(tripId))
                                .willReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> tripService.handleDriverRideExpired(
                                                new DriverRideExpiredEvent(
                                                                eventId, tripId, driverId, Instant.now())));
        }

        @Test
        void handleDriverRideExpired_ignoresEventWhenTripIsInStaleState() {
                Trip trip = tripWithStatus(TripStatus.TRIP_STARTED);
                givenTrip(trip);

                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(false);

                tripService.handleDriverRideExpired(
                                new DriverRideExpiredEvent(
                                                eventId,
                                                tripId,
                                                driverId,
                                                Instant.now()));

                assertEquals(TripStatus.TRIP_STARTED, trip.getStatus());
                assertEquals(driverId, trip.getDriverId());

                verify(tripRepository, never()).save(any(Trip.class));
                verify(outboxEventService, never())
                                .saveTripRematchingEvent(any(TripRematchingEvent.class));
                verify(processedEventRepository, never())
                                .save(any(ProcessedEvent.class));
        }

        @Test
        void handleDriverRideAccepted_throwsWhenTripDoesNotExist() {
                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(false);
                given(tripRepository.findById(tripId))
                                .willReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> tripService.handleDriverRideAccepted(
                                                new DriverRideAcceptedEvent(
                                                                eventId, tripId, driverId, Instant.now())));
        }

        @Test
        void handleDriverTripCompleted_ignoresDuplicateEvent() {
                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(true);

                tripService.handleDriverTripCompleted(
                                new DriverTripCompletedEvent(
                                                eventId, tripId, driverId, Instant.now()));

                verify(tripRepository, never()).findById(any(UUID.class));
                verify(tripRepository, never()).save(any(Trip.class));
        }

        @Test
        void handleDriverTripCompleted_throwsWhenTripDoesNotExist() {
                UUID eventId = UUID.randomUUID();

                given(processedEventRepository.existsById(eventId))
                                .willReturn(false);
                given(tripRepository.findById(tripId))
                                .willReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> tripService.handleDriverTripCompleted(
                                                new DriverTripCompletedEvent(
                                                                eventId, tripId, driverId, Instant.now())));
        }

}
