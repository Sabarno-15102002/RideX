package com.ridex.trip.service.Impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.trip.dto.PaymentSummary;
import com.ridex.trip.dto.TripStatus;
import com.ridex.trip.dto.request.CreateTripRequest;
import com.ridex.trip.dto.response.TripResponse;
import com.ridex.trip.entity.ProcessedEvent;
import com.ridex.trip.entity.RiderIdentity;
import com.ridex.trip.entity.Trip;
import com.ridex.trip.event.event.DriverArrivedEvent;
import com.ridex.trip.event.event.DriverMatchRequestedEvent;
import com.ridex.trip.event.event.DriverRideAcceptedEvent;
import com.ridex.trip.event.event.DriverRideExpiredEvent;
import com.ridex.trip.event.event.DriverRideRejectedEvent;
import com.ridex.trip.event.event.DriverTripCompletedEvent;
import com.ridex.trip.event.event.DriverTripStartedEvent;
import com.ridex.trip.event.event.TripCompletedEvent;
import com.ridex.trip.event.event.TripRematchingEvent;
import com.ridex.trip.event.event.TripRequestedEvent;
import com.ridex.trip.exception.IllegalOperationException;
import com.ridex.trip.exception.ResourceNotFoundException;
import com.ridex.trip.repository.ProcessedEventRepository;
import com.ridex.trip.repository.RiderIdentityRepository;
import com.ridex.trip.repository.TripPaymentRepository;
import com.ridex.trip.repository.TripRepository;
import com.ridex.trip.service.TripService;
import com.ridex.trip.service.util.OutboxEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j 
public class TripServiceImpl implements TripService {

        private final TripRepository tripRepository;
        private final RiderIdentityRepository riderIdentityRepository;
        private final OutboxEventService outboxEventService;
        private final ProcessedEventRepository processedEventRepository;
        private final TripPaymentRepository tripPaymentRepository;

        @Override
        @Transactional
        public TripResponse createTrip(
                        UUID userId,
                        CreateTripRequest request) {
                RiderIdentity riderIdentity = riderIdentityRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Rider profile not found"));

                Instant now = Instant.now();

                Trip trip = Trip.builder()
                                .riderId(riderIdentity.getRiderId())
                                .pickupLatitude(request.pickupLatitude())
                                .pickupLongitude(request.pickupLongitude())
                                .dropoffLatitude(request.dropoffLatitude())
                                .dropoffLongitude(request.dropoffLongitude())
                                .status(TripStatus.REQUESTED)
                                .requestedAt(now)
                                .build();

                log.info("Before save: tripId={}", trip.getId());

                Trip savedTrip = tripRepository.save(trip);

                log.info("After save: tripId={}", savedTrip.getId());

                TripRequestedEvent event = new TripRequestedEvent(
                                UUID.randomUUID(),
                                savedTrip.getId(),
                                savedTrip.getRiderId(),
                                request.pickupLatitude(),
                                request.pickupLongitude(),
                                request.dropoffLatitude(),
                                request.dropoffLongitude(),
                                now);

                outboxEventService.saveTripRequestedEvent(event);

                return toResponse(savedTrip);
        }

        private TripResponse toResponse(Trip trip) {
                return new TripResponse(
                                trip.getId(),
                                trip.getRiderId(),
                                trip.getDriverId(),
                                trip.getPickupLatitude(),
                                trip.getPickupLongitude(),
                                trip.getDropoffLatitude(),
                                trip.getDropoffLongitude(),
                                trip.getStatus(),
                                trip.getRequestedAt(),
                                buildPaymentSummary(trip.getId())
                );
        }

        private PaymentSummary buildPaymentSummary(UUID tripId) {

                return tripPaymentRepository.findById(tripId)
                                .map(payment -> new PaymentSummary(
                                                payment.getAmount(),
                                                payment.getCurrency(),
                                                payment.getStatus()))
                                .orElse(null);
        }

        @Override
        @Transactional
        public void assignDriver(DriverMatchRequestedEvent event) {

                if (processedEventRepository.existsById(event.eventId())) {
                        return;
                }

                Trip trip = tripRepository.findById(event.tripId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Trip not found: " + event.tripId()));

                if (trip.getStatus() != TripStatus.MATCHING
                                && trip.getStatus() != TripStatus.REQUESTED) {
                        return;
                }

                trip.setDriverId(event.driverId());
                trip.setStatus(TripStatus.DRIVER_ASSIGNED);
                trip.setAssignedAt(Instant.now());

                tripRepository.save(trip);

                processedEventRepository.save(
                                new ProcessedEvent(
                                                event.eventId(),
                                                Instant.now()));

        }

        @Override
        @Transactional
        public void handleDriverRideAccepted(DriverRideAcceptedEvent event) {

                if (processedEventRepository.existsById(event.eventId())) {
                        return;
                }

                Trip trip = tripRepository.findById(event.tripId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Trip not found: " + event.tripId()));

                if (trip.getStatus() != TripStatus.DRIVER_ASSIGNED) {
                        return;
                }

                if (!event.driverId().equals(trip.getDriverId())) {
                        throw new IllegalOperationException(
                                        "Accepted driver does not match assigned driver");
                }

                trip.setStatus(TripStatus.DRIVER_ARRIVING);
                trip.setUpdatedAt(Instant.now());

                tripRepository.save(trip);

                processedEventRepository.save(
                                new ProcessedEvent(
                                                event.eventId(),
                                                Instant.now()));
        }

        @Override
        @Transactional
        public void handleDriverRideRejected(DriverRideRejectedEvent event) {

                if (processedEventRepository.existsById(event.eventId())) {
                        return;
                }

                Trip trip = tripRepository.findById(event.tripId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Trip not found: " + event.tripId()));

                // Ignore stale/duplicate business events.
                if (trip.getStatus() != TripStatus.DRIVER_ASSIGNED) {
                        return;
                }

                // The rejecting driver must be the driver assigned to this trip.
                if (!event.driverId().equals(trip.getDriverId())) {
                        throw new IllegalOperationException(
                                        "Rejected driver does not match assigned driver");
                }

                trip.setStatus(TripStatus.MATCHING);
                trip.setDriverId(null);
                trip.setUpdatedAt(Instant.now());

                tripRepository.save(trip);

                TripRematchingEvent rematchingEvent = new TripRematchingEvent(
                                UUID.randomUUID(),
                                trip.getId(),
                                trip.getRiderId(),
                                event.driverId(),
                                trip.getPickupLatitude(),
                                trip.getPickupLongitude(),
                                trip.getDropoffLatitude(),
                                trip.getDropoffLongitude(),
                                Instant.now());

                outboxEventService.saveTripRematchingEvent(rematchingEvent);

                processedEventRepository.save(
                                new ProcessedEvent(
                                                event.eventId(),
                                                Instant.now()));
        }

        @Override
        @Transactional
        public void handleDriverRideExpired(DriverRideExpiredEvent event) {

                if (processedEventRepository.existsById(event.eventId())) {
                        return;
                }

                Trip trip = tripRepository.findById(event.tripId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Trip not found: " + event.tripId()));

                // The reservation that expired must belong to
                // the driver currently assigned to this trip.
                if (!event.driverId().equals(trip.getDriverId())) {
                        throw new IllegalOperationException(
                                        "Expired driver does not match assigned driver");
                }

                // Ignore stale events.
                if (trip.getStatus() != TripStatus.DRIVER_ASSIGNED) {
                        return;
                }

                Instant now = Instant.now();

                trip.setStatus(TripStatus.MATCHING);
                trip.setDriverId(null);
                trip.setUpdatedAt(now);

                tripRepository.save(trip);

                TripRematchingEvent rematchingEvent = new TripRematchingEvent(
                                UUID.randomUUID(),
                                trip.getId(),
                                trip.getRiderId(),
                                event.driverId(),
                                trip.getPickupLatitude(),
                                trip.getPickupLongitude(),
                                trip.getDropoffLatitude(),
                                trip.getDropoffLongitude(),
                                now);

                outboxEventService.saveTripRematchingEvent(rematchingEvent);

                processedEventRepository.save(
                                new ProcessedEvent(
                                                event.eventId(),
                                                now));
        }

        @Override
        @Transactional
        public void handleDriverArrived(DriverArrivedEvent event) {

                if (processedEventRepository.existsById(event.eventId())) {
                        return;
                }

                Trip trip = tripRepository.findById(event.tripId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Trip not found: " + event.tripId()));

                if (trip.getStatus() != TripStatus.DRIVER_ARRIVING) {
                        return;
                }

                if (!event.driverId().equals(trip.getDriverId())) {
                        throw new IllegalOperationException(
                                        "Arriving driver does not match assigned driver");
                }

                Instant now = Instant.now();

                trip.setStatus(TripStatus.DRIVER_ARRIVED);
                trip.setUpdatedAt(now);

                tripRepository.save(trip);

                processedEventRepository.save(
                                new ProcessedEvent(
                                                event.eventId(),
                                                now));
        }

        @Override
        @Transactional
        public void handleDriverTripStarted(DriverTripStartedEvent event) {

                if (processedEventRepository.existsById(event.eventId())) {
                        return;
                }

                Trip trip = tripRepository.findById(event.tripId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Trip not found: " + event.tripId()));

                if (trip.getStatus() != TripStatus.DRIVER_ARRIVED) {
                        return;
                }

                if (!event.driverId().equals(trip.getDriverId())) {
                        throw new IllegalOperationException(
                                        "Starting driver does not match assigned driver");
                }

                Instant now = Instant.now();

                trip.setStatus(TripStatus.TRIP_STARTED);
                trip.setStartedAt(event.startedAt());
                trip.setUpdatedAt(now);

                tripRepository.save(trip);

                processedEventRepository.save(
                                new ProcessedEvent(
                                                event.eventId(),
                                                now));
        }

        @Override
        @Transactional
        public void handleDriverTripCompleted(
                        DriverTripCompletedEvent event) {

                if (processedEventRepository.existsById(event.eventId())) {
                        return;
                }

                Trip trip = tripRepository.findById(event.tripId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Trip not found: " + event.tripId()));

                if (trip.getStatus() != TripStatus.TRIP_STARTED) {
                        return;
                }

                if (!event.driverId().equals(trip.getDriverId())) {
                        throw new IllegalOperationException(
                                        "Completing driver does not match assigned driver");
                }

                Instant now = Instant.now();

                trip.setStatus(TripStatus.TRIP_COMPLETED);
                trip.setCompletedAt(event.completedAt());
                trip.setUpdatedAt(now);

                tripRepository.save(trip);

                processedEventRepository.save(
                                new ProcessedEvent(
                                                event.eventId(),
                                                now));

                TripCompletedEvent tripCompletedEvent = new TripCompletedEvent(
                                UUID.randomUUID(),
                                trip.getId(),
                                trip.getRiderId(),
                                trip.getDriverId(),

                                trip.getPickupLatitude(),
                                trip.getPickupLongitude(),

                                trip.getDropoffLatitude(),
                                trip.getDropoffLongitude(),

                                trip.getStartedAt(),
                                event.completedAt());

                outboxEventService.saveTripCompletedEvent(tripCompletedEvent);

        }

        @Override
        public TripStatus geTripStatus(UUID tripId) {
                Trip trip = tripRepository.findById(tripId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Trip not found: " + tripId));

                return trip.getStatus();
        }
}