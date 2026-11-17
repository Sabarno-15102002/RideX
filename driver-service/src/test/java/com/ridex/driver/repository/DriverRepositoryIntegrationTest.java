package com.ridex.driver.repository;

import com.ridex.driver.entity.Driver;
import com.ridex.driver.utilities.DriverStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DriverRepositoryIntegrationTest {

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
    private DriverRepository driverRepository;

    @Test
    void saveDriver_shouldPersistSuccessfully() {

        Driver driver = createDriver();

        Driver saved = driverRepository.saveAndFlush(driver);

        assertThat(saved.getId()).isNotNull();
        assertThat(driverRepository.findById(saved.getId()))
                .isPresent();
    }

    @Test
    void saveDriver_shouldApplyDefaultValues() {

        Driver driver = createDriver();

        Driver saved = driverRepository.saveAndFlush(driver);

        assertThat(saved.getRating()).isEqualByComparingTo("5.00");
        assertThat(saved.getStatus()).isEqualTo(DriverStatus.OFFLINE);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByUserId_shouldReturnDriver() {

        Driver driver = driverRepository.saveAndFlush(createDriver());

        assertThat(driverRepository.findByUserId(driver.getUserId()))
                .isPresent()
                .get()
                .extracting(Driver::getId)
                .isEqualTo(driver.getId());
    }

    @Test
    void existsByUserId_shouldReturnTrueForExistingDriver() {

        Driver driver = driverRepository.saveAndFlush(createDriver());

        assertThat(driverRepository.existsByUserId(driver.getUserId()))
                .isTrue();
    }

    @Test
    void existsByLicenseNumber_shouldReturnTrueForExistingLicense() {

        Driver driver = driverRepository.saveAndFlush(createDriver());

        assertThat(driverRepository.existsByLicenseNumber(
                driver.getLicenseNumber())).isTrue();
    }

    @Test
    void findByStatus_shouldReturnMatchingDrivers() {

        Driver driver = createDriver();
        driver.setStatus(DriverStatus.AVAILABLE);

        driverRepository.saveAndFlush(driver);

        assertThat(driverRepository.findByStatus(DriverStatus.AVAILABLE))
                .extracting(Driver::getUserId)
                .contains(driver.getUserId());
    }

    @Test
    void saveDriver_withDuplicateUserId_shouldFail() {

        UUID userId = UUID.randomUUID();

        driverRepository.saveAndFlush(createDriver(userId, "LIC-1001"));

        assertThatThrownBy(() -> driverRepository.saveAndFlush(
                createDriver(userId, "LIC-1002"))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveDriver_withDuplicateLicenseNumber_shouldFail() {

        String license = "LIC-2001";

        driverRepository.saveAndFlush(
                createDriver(UUID.randomUUID(), license));

        assertThatThrownBy(() -> driverRepository.saveAndFlush(
                createDriver(UUID.randomUUID(), license))).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Driver createDriver() {
        return createDriver(UUID.randomUUID(), "LIC-" + UUID.randomUUID());
    }

    private Driver createDriver(UUID userId, String license) {
        return Driver.builder()
                .userId(userId)
                .name("Integration Driver")
                .licenseNumber(license)
                .build();
    }
}
