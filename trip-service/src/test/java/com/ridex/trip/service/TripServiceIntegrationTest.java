
package com.ridex.trip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridex.trip.config.TripMetrics;
import com.ridex.trip.dto.TripStatus;
import com.ridex.trip.dto.request.CreateTripRequest;
import com.ridex.trip.dto.response.TripResponse;
import com.ridex.trip.entity.*;
import com.ridex.trip.event.EventSerializer;
import com.ridex.trip.event.event.*;
import com.ridex.trip.repository.*;
import com.ridex.trip.service.Impl.TripServiceImpl;
import com.ridex.trip.service.util.OutboxEventService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@DataJpaTest(properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Execution(ExecutionMode.SAME_THREAD)
@Import({
                TripServiceImpl.class,
                OutboxEventService.class,
                EventSerializer.class,
                TripMetrics.class,
                TripServiceIntegrationTest.TestBeans.class
})
class TripServiceIntegrationTest {

        @Container
        static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
                        .withDatabaseName("trip_service_test_db")
                        .withUsername("test")
                        .withPassword("test");

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
                registry.add("spring.datasource.username", POSTGRES::getUsername);
                registry.add("spring.datasource.password", POSTGRES::getPassword);
        }

        @TestConfiguration
        static class TestBeans {

                @Bean
                @Primary
                ObjectMapper testObjectMapper() {
                        return new ObjectMapper()
                                        .registerModule(new JavaTimeModule());
                }

                @Bean
                @Primary
                MeterRegistry testMeterRegistry() {
                        return new SimpleMeterRegistry();
                }
        }

        @Autowired
        private TripService tripService;

        @Autowired
        private TripRepository tripRepository;

        @Autowired
        private RiderIdentityRepository riderIdentityRepository;

        @Autowired
        private ProcessedEventRepository processedEventRepository;

        @Autowired
        private OutboxEventRepository outboxEventRepository;

        @Autowired
        private TripPaymentRepository tripPaymentRepository;

        @MockitoSpyBean
        private OutboxEventService outboxEventService;

        @Autowired
        private PlatformTransactionManager transactionManager;

        @BeforeEach
        void cleanDatabase() {
                outboxEventRepository.deleteAllInBatch();
                processedEventRepository.deleteAllInBatch();
                tripPaymentRepository.deleteAllInBatch();
                tripRepository.deleteAllInBatch();
                riderIdentityRepository.deleteAllInBatch();
        }

        private TripResponse createTrip() {
                UUID userId = UUID.randomUUID();
                UUID riderId = UUID.randomUUID();

                riderIdentityRepository.saveAndFlush(
                                RiderIdentity.builder()
                                                .userId(userId)
                                                .riderId(riderId)
                                                .build());

                return tripService.createTrip(
                                userId,
                                new CreateTripRequest(
                                                12.9716,
                                                77.5946,
                                                12.9352,
                                                77.6245));
        }

        @Test
        void createTrip_shouldPersistTripAndOutboxEvent() {
                TripResponse response = createTrip();

                Trip persistedTrip = tripRepository.findById(response.id())
                                .orElseThrow();

                assertEquals(TripStatus.REQUESTED, persistedTrip.getStatus());
                assertNotNull(persistedTrip.getCreatedAt());

                assertEquals(1, outboxEventRepository.count());

                OutboxEvent outboxEvent = outboxEventRepository.findAll().get(0);

                assertEquals("TRIP_REQUESTED", outboxEvent.getEventType());
                assertEquals(response.id(), outboxEvent.getAggregateId());
                assertTrue(outboxEvent.getPayload().contains(
                                response.id().toString()));
        }

        @Test
        void tripLifecycle_shouldPersistEveryTransitionAndOutboxEvent() {
                TripResponse createdTrip = createTrip();

                UUID tripId = createdTrip.id();
                UUID driverId = UUID.randomUUID();

                Instant now = Instant.now();

                // Driver assigned
                tripService.assignDriver(
                                new DriverMatchRequestedEvent(
                                                UUID.randomUUID(),
                                                tripId,
                                                driverId,
                                                now));

                // Driver accepts
                tripService.handleDriverRideAccepted(
                                new DriverRideAcceptedEvent(
                                                UUID.randomUUID(),
                                                tripId,
                                                driverId,
                                                now));

                // Driver arrives
                tripService.handleDriverArrived(
                                new DriverArrivedEvent(
                                                UUID.randomUUID(),
                                                tripId,
                                                driverId,
                                                now));

                // Trip starts
                Instant startedAt = Instant.now();

                tripService.handleDriverTripStarted(
                                new DriverTripStartedEvent(
                                                UUID.randomUUID(),
                                                tripId,
                                                driverId,
                                                startedAt));

                // Trip completes
                Instant completedAt = startedAt.plusSeconds(900);

                tripService.handleDriverTripCompleted(
                                new DriverTripCompletedEvent(
                                                UUID.randomUUID(),
                                                tripId,
                                                driverId,
                                                completedAt));

                Trip completedTrip = tripRepository.findById(tripId)
                                .orElseThrow();

                assertEquals(
                                TripStatus.TRIP_COMPLETED,
                                completedTrip.getStatus());

                assertEquals(driverId, completedTrip.getDriverId());
                assertEquals(startedAt, completedTrip.getStartedAt());
                assertEquals(completedAt, completedTrip.getCompletedAt());

                // Five incoming driver events were processed.
                assertEquals(5, processedEventRepository.count());

                // Requested and completed events were written to the outbox.
                assertEquals(2, outboxEventRepository.count());
        }

        @Test
        void assignDriver_shouldBeIdempotentForDuplicateEvent() {
                TripResponse createdTrip = createTrip();

                UUID tripId = createdTrip.id();
                UUID driverId = UUID.randomUUID();
                UUID eventId = UUID.randomUUID();

                DriverMatchRequestedEvent event = new DriverMatchRequestedEvent(
                                eventId,
                                tripId,
                                driverId,
                                Instant.now());

                tripService.assignDriver(event);
                tripService.assignDriver(event);

                Trip trip = tripRepository.findById(tripId)
                                .orElseThrow();

                assertEquals(TripStatus.DRIVER_ASSIGNED, trip.getStatus());
                assertEquals(driverId, trip.getDriverId());

                assertEquals(1, processedEventRepository.count());
        }

        @Test
        void createTrip_whenOutboxSaveFails_shouldRollbackTrip() {

                UUID userId = UUID.randomUUID();
                UUID riderId = UUID.randomUUID();

                riderIdentityRepository.saveAndFlush(
                                RiderIdentity.builder()
                                                .userId(userId)
                                                .riderId(riderId)
                                                .build());

                // Simulate an outbox persistence failure.
                doThrow(new RuntimeException("Simulated outbox failure"))
                                .when(outboxEventService)
                                .saveTripRequestedEvent(any(TripRequestedEvent.class));

                CreateTripRequest request = new CreateTripRequest(
                                12.9716,
                                77.5946,
                                12.9352,
                                77.6245);

                assertThrows(
                                RuntimeException.class,
                                () -> tripService.createTrip(userId, request));

                // The trip must have rolled back.
                assertEquals(0, tripRepository.count());

                // No outbox event should have been committed.
                assertEquals(0, outboxEventRepository.count());

                // The independently committed rider identity should remain.
                assertEquals(1, riderIdentityRepository.count());
        }

        @Test
        void competingDriverAssignments_shouldAllowOnlyOneTransactionToCommit()
                        throws Exception {

                TripResponse createdTrip = createTrip();
                UUID tripId = createdTrip.id();

                UUID driverA = UUID.randomUUID();
                UUID driverB = UUID.randomUUID();

                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

                CountDownLatch bothTransactionsLoaded = new CountDownLatch(2);
                CountDownLatch startUpdates = new CountDownLatch(1);

                ExecutorService executor = Executors.newFixedThreadPool(2);

                try {
                        Future<Boolean> first = executor.submit(() -> attemptAssignment(
                                        tripId,
                                        driverA,
                                        transactionTemplate,
                                        bothTransactionsLoaded,
                                        startUpdates));

                        Future<Boolean> second = executor.submit(() -> attemptAssignment(
                                        tripId,
                                        driverB,
                                        transactionTemplate,
                                        bothTransactionsLoaded,
                                        startUpdates));

                        // Ensure both transactions read the same initial version.
                        assertTrue(
                                        bothTransactionsLoaded.await(10, TimeUnit.SECONDS),
                                        "Both transactions should load the trip");

                        // Allow both transactions to attempt their updates.
                        startUpdates.countDown();

                        boolean firstCommitted = first.get(20, TimeUnit.SECONDS);
                        boolean secondCommitted = second.get(20, TimeUnit.SECONDS);

                        // Exactly one transaction must commit.
                        assertNotEquals(
                                        firstCommitted,
                                        secondCommitted,
                                        "Only one driver assignment should succeed");

                        Trip finalTrip = tripRepository.findById(tripId)
                                        .orElseThrow();

                        assertEquals(
                                        TripStatus.DRIVER_ASSIGNED,
                                        finalTrip.getStatus());

                        assertTrue(
                                        finalTrip.getDriverId().equals(driverA)
                                                        || finalTrip.getDriverId().equals(driverB));

                } finally {
                        startUpdates.countDown();
                        executor.shutdownNow();
                }
        }

        private boolean attemptAssignment(
                        UUID tripId,
                        UUID driverId,
                        TransactionTemplate transactionTemplate,
                        CountDownLatch bothTransactionsLoaded,
                        CountDownLatch startUpdates) {

                try {
                        transactionTemplate.executeWithoutResult(status -> {

                                Trip trip = tripRepository.findById(tripId)
                                                .orElseThrow();

                                // Signal that this transaction has read the trip.
                                bothTransactionsLoaded.countDown();

                                try {
                                        if (!startUpdates.await(10, TimeUnit.SECONDS)) {
                                                throw new IllegalStateException(
                                                                "Timed out waiting to start concurrent updates");
                                        }
                                } catch (InterruptedException exception) {
                                        Thread.currentThread().interrupt();
                                        throw new IllegalStateException(exception);
                                }

                                trip.setDriverId(driverId);
                                trip.setStatus(TripStatus.DRIVER_ASSIGNED);
                                trip.setAssignedAt(Instant.now());

                                tripRepository.saveAndFlush(trip);
                        });

                        return true;

                } catch (OptimisticLockingFailureException exception) {
                        return false;
                }
        }

        @Test
        void competingDriverEvents_shouldNotOverwriteDriverAssignment()
                        throws Exception {

                TripResponse createdTrip = createTrip();

                UUID tripId = createdTrip.id();

                UUID driverA = UUID.randomUUID();
                UUID driverB = UUID.randomUUID();

                DriverMatchRequestedEvent eventA = new DriverMatchRequestedEvent(
                                UUID.randomUUID(),
                                tripId,
                                driverA,
                                Instant.now());

                DriverMatchRequestedEvent eventB = new DriverMatchRequestedEvent(
                                UUID.randomUUID(),
                                tripId,
                                driverB,
                                Instant.now());

                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);

                ExecutorService executor = Executors.newFixedThreadPool(2);

                try {
                        Future<?> first = executor.submit(
                                        () -> assignConcurrently(eventA, ready, start));

                        Future<?> second = executor.submit(
                                        () -> assignConcurrently(eventB, ready, start));

                        assertTrue(
                                        ready.await(10, TimeUnit.SECONDS),
                                        "Both assignment tasks should be ready");

                        start.countDown();

                        first.get(20, TimeUnit.SECONDS);
                        second.get(20, TimeUnit.SECONDS);

                        Trip finalTrip = tripRepository.findById(tripId)
                                        .orElseThrow();

                        assertEquals(
                                        TripStatus.DRIVER_ASSIGNED,
                                        finalTrip.getStatus());

                        assertTrue(
                                        driverA.equals(finalTrip.getDriverId())
                                                        || driverB.equals(finalTrip.getDriverId()),
                                        "The assigned driver must be one of the competing drivers");

                        assertEquals(
                                        1,
                                        processedEventRepository.count(),
                                        "Only the winning assignment should be recorded as processed");

                } finally {
                        start.countDown();
                        executor.shutdownNow();
                }
        }

        private void assignConcurrently(
                        DriverMatchRequestedEvent event,
                        CountDownLatch ready,
                        CountDownLatch start) {
                ready.countDown();

                try {
                        if (!start.await(10, TimeUnit.SECONDS)) {
                                throw new IllegalStateException(
                                                "Timed out waiting to start assignment");
                        }

                        tripService.assignDriver(event);

                } catch (OptimisticLockingFailureException exception) {
                        // Expected when another transaction wins the version conflict.
                } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                }
        }

        @Test
        void assignDriver_whenAnotherDriverIsAlreadyAssigned_shouldIgnoreAndRecordEvent() {

                TripResponse createdTrip = createTrip();

                UUID tripId = createdTrip.id();

                UUID winningDriver = UUID.randomUUID();
                UUID losingDriver = UUID.randomUUID();

                tripService.assignDriver(
                                new DriverMatchRequestedEvent(
                                                UUID.randomUUID(),
                                                tripId,
                                                winningDriver,
                                                Instant.now()));

                UUID losingEventId = UUID.randomUUID();

                tripService.assignDriver(
                                new DriverMatchRequestedEvent(
                                                losingEventId,
                                                tripId,
                                                losingDriver,
                                                Instant.now()));

                Trip finalTrip = tripRepository.findById(tripId)
                                .orElseThrow();

                assertEquals(TripStatus.DRIVER_ASSIGNED, finalTrip.getStatus());
                assertEquals(winningDriver, finalTrip.getDriverId());

                assertTrue(processedEventRepository.existsById(losingEventId));
                assertEquals(2, processedEventRepository.count());
        }
}
