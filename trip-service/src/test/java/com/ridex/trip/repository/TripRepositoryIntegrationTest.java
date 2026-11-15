
package com.ridex.trip.repository;

import com.ridex.trip.dto.TripStatus;
import com.ridex.trip.entity.Trip;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class TripRepositoryIntegrationTest {

        @Container
        static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
                        .withDatabaseName("trip_test_db")
                        .withUsername("test")
                        .withPassword("test");

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {

                registry.add(
                                "spring.datasource.url",
                                POSTGRES::getJdbcUrl);

                registry.add(
                                "spring.datasource.username",
                                POSTGRES::getUsername);

                registry.add(
                                "spring.datasource.password",
                                POSTGRES::getPassword);
        }

        @Autowired
        private TripRepository tripRepository;

        @Autowired
        private EntityManager entityManager;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private Trip createTrip() {

                return Trip.builder()
                                .riderId(UUID.randomUUID())
                                .pickupLatitude(12.9716)
                                .pickupLongitude(77.5946)
                                .dropoffLatitude(12.9352)
                                .dropoffLongitude(77.6245)
                                .status(TripStatus.REQUESTED)
                                .requestedAt(Instant.now())
                                .build();
        }

        // ---------------------------------------------
        // PERSISTENCE
        // ---------------------------------------------

        @Test
        void saveTrip_shouldPersistAndGenerateId() {

                Trip trip = createTrip();

                Trip savedTrip = tripRepository.saveAndFlush(trip);

                assertNotNull(savedTrip.getId());
                assertEquals(trip.getRiderId(), savedTrip.getRiderId());
                assertEquals(
                                TripStatus.REQUESTED,
                                savedTrip.getStatus());

                assertNotNull(savedTrip.getCreatedAt());
                assertNotNull(savedTrip.getUpdatedAt());
        }

        @Test
        void findById_shouldReturnPersistedTrip() {

                Trip trip = createTrip();

                Trip savedTrip = tripRepository.saveAndFlush(trip);

                UUID tripId = savedTrip.getId();

                entityManager.clear();

                Trip retrievedTrip = tripRepository.findById(tripId)
                                .orElseThrow();

                assertEquals(tripId, retrievedTrip.getId());
                assertEquals(trip.getRiderId(), retrievedTrip.getRiderId());

                assertEquals(12.9716, retrievedTrip.getPickupLatitude());
                assertEquals(77.5946, retrievedTrip.getPickupLongitude());

                assertEquals(12.9352, retrievedTrip.getDropoffLatitude());
                assertEquals(77.6245, retrievedTrip.getDropoffLongitude());

                assertEquals(
                                TripStatus.REQUESTED,
                                retrievedTrip.getStatus());
        }

        // ---------------------------------------------
        // ENTITY LIFECYCLE CALLBACKS
        // ---------------------------------------------

        @Test
        void saveTrip_shouldPopulateDefaultValues() {

                Trip trip = Trip.builder()
                                .riderId(UUID.randomUUID())
                                .pickupLatitude(12.9716)
                                .pickupLongitude(77.5946)
                                .dropoffLatitude(12.9352)
                                .dropoffLongitude(77.6245)
                                .build();

                Trip savedTrip = tripRepository.saveAndFlush(trip);

                assertNotNull(savedTrip.getId());

                assertEquals(
                                TripStatus.REQUESTED,
                                savedTrip.getStatus());

                assertNotNull(savedTrip.getRequestedAt());
                assertNotNull(savedTrip.getCreatedAt());
                assertNotNull(savedTrip.getUpdatedAt());
        }

        @Test
        void updateTrip_shouldPersistUpdatedStatusAndTimestamp() {

                Trip trip = createTrip();

                Trip savedTrip = tripRepository.saveAndFlush(trip);

                UUID tripId = savedTrip.getId();

                Instant previousUpdatedAt = savedTrip.getUpdatedAt();

                entityManager.clear();

                Trip existingTrip = tripRepository.findById(tripId)
                                .orElseThrow();

                existingTrip.setStatus(TripStatus.DRIVER_ASSIGNED);
                existingTrip.setDriverId(UUID.randomUUID());
                existingTrip.setAssignedAt(Instant.now());

                tripRepository.saveAndFlush(existingTrip);

                entityManager.clear();

                Trip updatedTrip = tripRepository.findById(tripId)
                                .orElseThrow();

                assertEquals(
                                TripStatus.DRIVER_ASSIGNED,
                                updatedTrip.getStatus());

                assertNotNull(updatedTrip.getDriverId());
                assertNotNull(updatedTrip.getAssignedAt());

                assertNotNull(updatedTrip.getUpdatedAt());
                assertFalse(
                                updatedTrip.getUpdatedAt().isBefore(previousUpdatedAt));
        }

        // ---------------------------------------------
        // POSTGRESQL ENUM PERSISTENCE
        // ---------------------------------------------

        @Test
        void saveTrip_shouldPersistStatusAsString() {

                Trip trip = createTrip();

                Trip savedTrip = tripRepository.saveAndFlush(trip);

                String persistedStatus = jdbcTemplate.queryForObject(
                                "SELECT status FROM trips WHERE id = ?",
                                String.class,
                                savedTrip.getId());

                assertEquals("REQUESTED", persistedStatus);
        }

        // ---------------------------------------------
        // DATABASE CONSTRAINTS
        // ---------------------------------------------

        @Test
        void saveTrip_shouldRejectMissingRiderId() {

                Trip trip = createTrip();
                trip.setRiderId(null);

                assertThrows(
                                RuntimeException.class,
                                () -> tripRepository.saveAndFlush(trip));
        }
}
