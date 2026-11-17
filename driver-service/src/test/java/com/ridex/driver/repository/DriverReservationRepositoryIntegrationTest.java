
package com.ridex.driver.repository;

import com.ridex.driver.entity.DriverReservation;
import com.ridex.driver.utilities.ReservationStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DriverReservationRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private DriverReservationRepository reservationRepository;

    @Test
    void tryCreateActiveReservation_shouldInsertReservation() {

        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        int inserted = reservationRepository.tryCreateActiveReservation(
                UUID.randomUUID(),
                driverId,
                tripId,
                Instant.now().plusSeconds(15));

        assertThat(inserted).isEqualTo(1);

        assertThat(reservationRepository.findByTripIdAndStatus(
                tripId,
                ReservationStatus.ACTIVE)).isPresent();
    }

    @Test
    void tryCreateActiveReservation_withSameTrip_shouldNotInsertDuplicate() {

        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        int first = reservationRepository.tryCreateActiveReservation(
                UUID.randomUUID(),
                driverId,
                tripId,
                Instant.now().plusSeconds(15));

        int second = reservationRepository.tryCreateActiveReservation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                tripId,
                Instant.now().plusSeconds(15));

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
    }

    @Test
    void tryCreateActiveReservation_withSameDriverAndDifferentTrip_shouldNotInsert()
            throws Exception {

        UUID driverId = UUID.randomUUID();

        int first = reservationRepository.tryCreateActiveReservation(
                UUID.randomUUID(),
                driverId,
                UUID.randomUUID(),
                Instant.now().plusSeconds(15));

        int second = reservationRepository.tryCreateActiveReservation(
                UUID.randomUUID(),
                driverId,
                UUID.randomUUID(),
                Instant.now().plusSeconds(15));

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
    }

    @Test
    void releasedReservation_shouldAllowDriverToBeReservedAgain() {

        UUID driverId = UUID.randomUUID();

        DriverReservation released = createReservation(
                driverId,
                UUID.randomUUID(),
                ReservationStatus.RELEASED,
                Instant.now().plusSeconds(15));

        reservationRepository.saveAndFlush(released);

        int inserted = reservationRepository.tryCreateActiveReservation(
                UUID.randomUUID(),
                driverId,
                UUID.randomUUID(),
                Instant.now().plusSeconds(15));

        assertThat(inserted).isEqualTo(1);
    }

    @Test
    void expiredReservation_shouldAllowDriverToBeReservedAgain() {

        UUID driverId = UUID.randomUUID();

        reservationRepository.saveAndFlush(
                createReservation(
                        driverId,
                        UUID.randomUUID(),
                        ReservationStatus.EXPIRED,
                        Instant.now().minusSeconds(10)));

        int inserted = reservationRepository.tryCreateActiveReservation(
                UUID.randomUUID(),
                driverId,
                UUID.randomUUID(),
                Instant.now().plusSeconds(15));

        assertThat(inserted).isEqualTo(1);
    }

    @Test
    void findByTripIdAndStatus_shouldReturnActiveReservation() {

        DriverReservation reservation = reservationRepository.saveAndFlush(
                createReservation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ReservationStatus.ACTIVE,
                        Instant.now().plusSeconds(15)));

        assertThat(reservationRepository.findByTripIdAndStatus(
                reservation.getTripId(),
                ReservationStatus.ACTIVE)).isPresent();
    }

    @Test
    void findByTripIdAndStatusForUpdate_shouldFindActiveReservation() {

        DriverReservation reservation = reservationRepository.saveAndFlush(
                createReservation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ReservationStatus.ACTIVE,
                        Instant.now().plusSeconds(15)));

        assertThat(reservationRepository.findByTripIdAndStatusForUpdate(
                reservation.getTripId(),
                ReservationStatus.ACTIVE)).isPresent();
    }

    @Test
    void findExpiredReservations_shouldReturnOnlyExpiredActiveReservations() {

        Instant now = Instant.now();

        DriverReservation expired = reservationRepository.saveAndFlush(
                createReservation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ReservationStatus.ACTIVE,
                        now.minusSeconds(10)));

        reservationRepository.saveAndFlush(
                createReservation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ReservationStatus.ACTIVE,
                        now.plusSeconds(60)));

        reservationRepository.saveAndFlush(
                createReservation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ReservationStatus.RELEASED,
                        now.minusSeconds(10)));

        assertThat(reservationRepository.findExpiredReservations(
                ReservationStatus.ACTIVE,
                now,
                PageRequest.of(0, 100))).extracting(DriverReservation::getId)
                .containsExactly(expired.getId());
    }

    private DriverReservation createReservation(
            UUID driverId,
            UUID tripId,
            ReservationStatus status,
            Instant expiresAt) {

        return DriverReservation.builder()
                .driverId(driverId)
                .tripId(tripId)
                .status(status)
                .expiresAt(expiresAt)
                .build();
    }
}
