
package com.ridex.auth.integration;

import com.ridex.auth.dto.request.RegisterRequest;
import com.ridex.auth.entity.OutboxEvent;
import com.ridex.auth.entity.User;
import com.ridex.auth.event.EventSerializer;
import com.ridex.auth.repository.OutboxEventRepository;
import com.ridex.auth.repository.UserRepository;
import com.ridex.auth.service.AuthService;
import com.ridex.auth.utilities.UserRole;
import com.ridex.auth.utilities.UserStatus;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "outbox.publisher.fixed-delay-ms=3600000",
        "jwt.secret=integration-test-secret-key-must-be-at-least-32-bytes",
        "jwt.expiration-ms=900000",
        "refresh-token.expiration-ms=2592000000"
})
class AuthServicePostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("auth_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private Flyway flyway;

    @MockitoBean
    private EventSerializer eventSerializer;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        userRepository.deleteAll();

        when(eventSerializer.serialize(any()))
                .thenReturn("{\"event\":\"USER_REGISTERED\"}");
    }

    private RegisterRequest registrationRequest(String email) {
        return new RegisterRequest(
                "Integration Rider",
                email,
                "StrongPassword123!",
                "9876543210",
                UserRole.RIDER
        );
    }

    @Test
    void flyway_shouldApplyAllMigrations() {
        assertNotNull(flyway.info().current());
        assertEquals("4", flyway.info().current().getVersion().getVersion());
    }

    @Test
    void register_shouldPersistUserInPostgres() {
        var response = authService.register(
                registrationRequest("rider1@test.com"));

        User saved = userRepository.findByEmail("rider1@test.com")
                .orElseThrow();

        assertEquals(response.userId(), saved.getId());
        assertEquals("Integration Rider", saved.getName());
        assertEquals(UserRole.RIDER, saved.getRole());
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertNotEquals("StrongPassword123!", saved.getPasswordHash());
    }

    @Test
    void register_shouldPersistOutboxEvent() {
        var response = authService.register(
                registrationRequest("rider2@test.com"));

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        assertEquals(1, events.size());

        OutboxEvent event = events.getFirst();

        assertEquals("USER", event.getAggregateType());
        assertEquals(response.userId(), event.getAggregateId());
        assertEquals("USER_REGISTERED", event.getEventType());
        assertEquals("{\"event\":\"USER_REGISTERED\"}", event.getPayload());
        assertNotNull(event.getCreatedAt());
        assertNull(event.getPublishedAt());
    }

    @Test
    void register_shouldPersistUserAndOutboxWithSameAggregateId() {
        var response = authService.register(
                registrationRequest("rider3@test.com"));

        OutboxEvent event = outboxEventRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()
                .getFirst();

        assertTrue(userRepository.existsById(response.userId()));
        assertEquals(response.userId(), event.getAggregateId());
    }

    @Test
    void register_shouldRollbackUserWhenOutboxSerializationFails() {
        when(eventSerializer.serialize(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

        assertThrows(RuntimeException.class,
                () -> authService.register(
                        registrationRequest("rollback@test.com")));

        assertFalse(userRepository.existsByEmail("rollback@test.com"));
        assertTrue(outboxEventRepository.findAll().isEmpty());
    }

    @Test
    void register_shouldEnforceUniqueEmailInPostgres() {
        authService.register(registrationRequest("duplicate@test.com"));

        assertThrows(RuntimeException.class,
                () -> authService.register(
                        registrationRequest("duplicate@test.com")));

        assertEquals(1, userRepository.count());
    }
}
