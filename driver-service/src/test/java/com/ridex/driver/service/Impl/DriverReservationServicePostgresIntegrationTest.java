package com.ridex.driver.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.DriverReservation;
import com.ridex.driver.entity.OutboxEvent;
import com.ridex.driver.event.EventSerializer;
import com.ridex.driver.exception.ResourceNotFoundException;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.DriverReservationRepository;
import com.ridex.driver.repository.OutboxEventRepository;
import com.ridex.driver.service.utilities.OutboxEventService;
import com.ridex.driver.utilities.DriverStatus;
import com.ridex.driver.utilities.ReservationStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        DriverReservationServiceImpl.class,
        OutboxEventService.class,
        EventSerializer.class,
        DriverReservationServicePostgresIntegrationTest.TestConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DriverReservationServicePostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private DriverReservationServiceImpl reservationService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private DriverReservationRepository reservationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAllInBatch();
        reservationRepository.deleteAllInBatch();
        driverRepository.deleteAllInBatch();
    }

    @Test
    void reserveDriver_shouldPersistActiveReservation() {

        Driver driver = saveAvailableDriver();
        UUID tripId = UUID.randomUUID();

        boolean result = reservationService.reserveDriver(
                driver.getId(),
                tripId
        );

        assertThat(result).isTrue();

        DriverReservation reservation =
                reservationRepository.findByTripId(tripId).orElseThrow();

        assertThat(reservation.getDriverId()).isEqualTo(driver.getId());
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plusSeconds(20));
    }

    @Test
    void reserveDriver_sameDriverAndSameTrip_shouldBeIdempotent() {

        Driver driver = saveAvailableDriver();
        UUID tripId = UUID.randomUUID();

        assertThat(reservationService.reserveDriver(
                driver.getId(), tripId
        )).isTrue();

        assertThat(reservationService.reserveDriver(
                driver.getId(), tripId
        )).isTrue();

        assertThat(reservationRepository.findAll())
                .hasSize(1);
    }

    @Test
    void reserveDriver_differentDriverForSameTrip_shouldReturnFalse() {

        Driver firstDriver = saveAvailableDriver();
        Driver secondDriver = saveAvailableDriver();
        UUID tripId = UUID.randomUUID();

        assertThat(reservationService.reserveDriver(
                firstDriver.getId(), tripId
        )).isTrue();

        assertThat(reservationService.reserveDriver(
                secondDriver.getId(), tripId
        )).isFalse();

        assertThat(reservationRepository.findAll())
                .hasSize(1);
    }

    @Test
    void reserveDriver_driverAlreadyHasActiveReservation_shouldReturnFalse() {

        Driver driver = saveAvailableDriver();

        assertThat(reservationService.reserveDriver(
                driver.getId(), UUID.randomUUID()
        )).isTrue();

        assertThat(reservationService.reserveDriver(
                driver.getId(), UUID.randomUUID()
        )).isFalse();

        assertThat(reservationRepository.findAll())
                .hasSize(1);
    }

    @Test
    void reserveDriver_nonAvailableDriver_shouldReturnFalse() {

        Driver driver = saveAvailableDriver();
        driver.setStatus(DriverStatus.ON_TRIP);
        driverRepository.saveAndFlush(driver);

        assertThat(reservationService.reserveDriver(
                driver.getId(), UUID.randomUUID()
        )).isFalse();

        assertThat(reservationRepository.findAll()).isEmpty();
    }

    @Test
    void acceptReservation_shouldUpdateReservationDriverAndOutbox() {

        Driver driver = saveAvailableDriver();
        UUID tripId = UUID.randomUUID();

        reservationService.reserveDriver(driver.getId(), tripId);

        reservationService.acceptReservation(driver.getId(), tripId);

        DriverReservation reservation =
                reservationRepository.findByTripId(tripId).orElseThrow();

        Driver updatedDriver =
                driverRepository.findById(driver.getId()).orElseThrow();

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.ACCEPTED);

        assertThat(updatedDriver.getStatus())
                .isEqualTo(DriverStatus.ON_TRIP);

        assertThat(outboxEventRepository.findAll())
                .hasSize(1)
                .extracting(OutboxEvent::getEventType)
                .containsExactly("DRIVER_RIDE_ACCEPTED");
    }

    @Test
    void acceptReservation_expiredReservation_shouldPersistExpiredStatus() {

        Driver driver = saveAvailableDriver();
        UUID tripId = UUID.randomUUID();

        saveReservation(
                driver.getId(),
                tripId,
                ReservationStatus.ACTIVE,
                Instant.now().minusSeconds(10)
        );

        assertThatThrownBy(() ->
                reservationService.acceptReservation(driver.getId(), tripId)
        ).isInstanceOf(ResourceNotFoundException.class)
         .hasMessageContaining("expired");

        DriverReservation reservation =
                reservationRepository.findByTripId(tripId).orElseThrow();

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.EXPIRED);

        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void acceptReservation_wrongDriver_shouldNotChangeReservation() {

        Driver owner = saveAvailableDriver();
        Driver anotherDriver = saveAvailableDriver();
        UUID tripId = UUID.randomUUID();

        reservationService.reserveDriver(owner.getId(), tripId);

        assertThatThrownBy(() ->
                reservationService.acceptReservation(
                        anotherDriver.getId(), tripId
                )
        ).isInstanceOf(ResourceNotFoundException.class);

        assertThat(reservationRepository.findByTripId(tripId).orElseThrow()
                .getStatus()).isEqualTo(ReservationStatus.ACTIVE);

        assertThat(driverRepository.findById(owner.getId()).orElseThrow()
                .getStatus()).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void releaseReservation_shouldPersistReleasedStatusAndOutbox() {

        Driver driver = saveAvailableDriver();
        UUID tripId = UUID.randomUUID();

        reservationService.reserveDriver(driver.getId(), tripId);

        reservationService.releaseReservation(driver.getId(), tripId);

        assertThat(reservationRepository.findByTripId(tripId).orElseThrow()
                .getStatus()).isEqualTo(ReservationStatus.RELEASED);

        assertThat(outboxEventRepository.findAll())
                .hasSize(1)
                .extracting(OutboxEvent::getEventType)
                .containsExactly("DRIVER_RIDE_REJECTED");
    }

    @Test
    void expireReservations_shouldPersistExpiredStatusAndOutbox() {

        Driver driver = saveAvailableDriver();
        UUID tripId = UUID.randomUUID();

        DriverReservation reservation = saveReservation(
                driver.getId(),
                tripId,
                ReservationStatus.ACTIVE,
                Instant.now().minusSeconds(10)
        );

        reservationService.expireReservations();

        assertThat(reservationRepository.findById(reservation.getId())
                .orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.EXPIRED);

        assertThat(outboxEventRepository.findAll())
                .hasSize(1)
                .extracting(OutboxEvent::getEventType)
                .containsExactly("DRIVER_RIDE_EXPIRED");
    }

    @Test
    void findDriverReservation_shouldReturnPersistedReservation() {

        Driver driver = saveAvailableDriver();
        UUID tripId = UUID.randomUUID();

        reservationService.reserveDriver(driver.getId(), tripId);

        assertThat(reservationService.findDriverReservation(tripId))
                .isNotNull()
                .extracting(DriverReservation::getTripId)
                .isEqualTo(tripId);
    }

    private Driver saveAvailableDriver() {
        Driver driver = Driver.builder()
                .userId(UUID.randomUUID())
                .name("Integration Driver")
                .licenseNumber("LIC-" + UUID.randomUUID())
                .status(DriverStatus.AVAILABLE)
                .build();

        return driverRepository.saveAndFlush(driver);
    }

    private DriverReservation saveReservation(
            UUID driverId,
            UUID tripId,
            ReservationStatus status,
            Instant expiresAt) {

        DriverReservation reservation = DriverReservation.builder()
                .driverId(driverId)
                .tripId(tripId)
                .status(status)
                .expiresAt(expiresAt)
                .build();

        return reservationRepository.saveAndFlush(reservation);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule());
        }
    }
}
