package com.ridex.driver.service.Impl;

import com.ridex.driver.entity.Driver;
import com.ridex.driver.entity.ProcessedEvent;
import com.ridex.driver.event.TripCompletedEvent;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.ProcessedEventRepository;
import com.ridex.driver.service.DriverReservationService;
import com.ridex.driver.service.DriverService;
import com.ridex.driver.service.utilities.OutboxEventService;
import com.ridex.driver.utilities.DriverStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@Testcontainers
@Import(DriverTripServiceImpl.class)
class DriverTripServicePostgresIntegrationTest {

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
    private DriverTripServiceImpl driverTripService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @MockitoBean
    private OutboxEventService outboxEventService;

    @MockitoBean
    private DriverReservationService reservationService;

    @MockitoBean
    private DriverService driverService;

    @BeforeEach
    void cleanDatabase() {
        processedEventRepository.deleteAllInBatch();
        driverRepository.deleteAllInBatch();
    }

    @Test
    void handleTripCompleted_shouldPersistDriverStatusAndProcessedEvent() {

        Driver driver = saveDriver(DriverStatus.ON_TRIP);
        UUID eventId = UUID.randomUUID();

        TripCompletedEvent event = createEvent(eventId, driver.getId());

        driverTripService.handleTripCompleted(event);

        Driver updatedDriver =
                driverRepository.findById(driver.getId()).orElseThrow();

        assertThat(updatedDriver.getStatus())
                .isEqualTo(DriverStatus.AVAILABLE);

        assertThat(processedEventRepository.existsById(eventId))
                .isTrue();

        verify(driverService).updateStatus(
                driver.getId(),
                DriverStatus.AVAILABLE
        );
    }

    @Test
    void handleTripCompleted_duplicateEvent_shouldNotProcessAgain() {

        Driver driver = saveDriver(DriverStatus.ON_TRIP);
        UUID eventId = UUID.randomUUID();

        processedEventRepository.saveAndFlush(
                new ProcessedEvent(eventId, Instant.now())
        );

        driverTripService.handleTripCompleted(
                createEvent(eventId, driver.getId())
        );

        Driver unchangedDriver =
                driverRepository.findById(driver.getId()).orElseThrow();

        assertThat(unchangedDriver.getStatus())
                .isEqualTo(DriverStatus.ON_TRIP);

        verifyNoInteractions(driverService);
    }

    @Test
    void handleTripCompleted_driverNotOnTrip_shouldNotChangeStatus() {

        Driver driver = saveDriver(DriverStatus.AVAILABLE);
        UUID eventId = UUID.randomUUID();

        driverTripService.handleTripCompleted(
                createEvent(eventId, driver.getId())
        );

        assertThat(driverRepository.findById(driver.getId())
                .orElseThrow().getStatus())
                .isEqualTo(DriverStatus.AVAILABLE);

        assertThat(processedEventRepository.existsById(eventId))
                .isFalse();

        verifyNoInteractions(driverService);
    }

    private Driver saveDriver(DriverStatus status) {
        return driverRepository.saveAndFlush(
                Driver.builder()
                        .userId(UUID.randomUUID())
                        .name("Integration Driver")
                        .licenseNumber("LIC-" + UUID.randomUUID())
                        .status(status)
                        .build()
        );
    }

    private TripCompletedEvent createEvent(UUID eventId, UUID driverId) {
        Instant now = Instant.now();

        return new TripCompletedEvent(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                driverId,
                12.9716,
                77.5946,
                12.9352,
                77.6245,
                now.minusSeconds(600),
                now
        );
    }
}
