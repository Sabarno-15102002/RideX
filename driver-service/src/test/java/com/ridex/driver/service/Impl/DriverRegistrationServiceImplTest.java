
package com.ridex.driver.service.Impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import com.ridex.driver.event.UserRegisteredEvent;
import com.ridex.driver.repository.DriverRepository;
import com.ridex.driver.repository.ProcessedEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverRegistrationServiceImplTest {

        @Mock
        private DriverRepository driverRepository;

        @Mock
        private ProcessedEventRepository processedEventRepository;

        @InjectMocks
        private DriverRegistrationServiceImpl registrationService;

        private UUID eventId;
        private UUID userId;
        private UserRegisteredEvent event;

        @BeforeEach
        void setUp() {
                eventId = UUID.randomUUID();
                userId = UUID.randomUUID();

                event = new UserRegisteredEvent(
                                eventId,
                                userId,
                                "John",
                                "DRIVER",
                                Instant.now());
        }

        @Test
        void handleUserRegistered_shouldCreateDriverAndMarkEventProcessed() {
                when(processedEventRepository.existsById(eventId))
                                .thenReturn(false);

                when(driverRepository.existsByUserId(userId))
                                .thenReturn(false);

                registrationService.handleUserRegistered(event);

                verify(driverRepository).save(argThat(driver -> driver.getUserId().equals(userId)
                                && driver.getName().equals("John")));

                verify(processedEventRepository).save(argThat(processed -> processed.getEventId().equals(eventId)));
        }

        @Test
        void handleUserRegistered_shouldIgnoreAlreadyProcessedEvent() {
                when(processedEventRepository.existsById(eventId))
                                .thenReturn(true);

                registrationService.handleUserRegistered(event);

                verifyNoInteractions(driverRepository);
                verify(processedEventRepository, never()).save(any());
        }

        @Test
        void handleUserRegistered_shouldIgnoreNonDriverRegistration() {
                UserRegisteredEvent riderEvent = new UserRegisteredEvent(
                                eventId,
                                userId,
                                "John",
                                "RIDER",
                                Instant.now());

                when(processedEventRepository.existsById(eventId))
                                .thenReturn(false);

                registrationService.handleUserRegistered(riderEvent);

                verifyNoInteractions(driverRepository);

                verify(processedEventRepository).save(argThat(processed -> processed.getEventId().equals(eventId)));
        }

        @Test
        void handleUserRegistered_shouldIgnoreExistingDriver() {
                when(processedEventRepository.existsById(eventId))
                                .thenReturn(false);

                when(driverRepository.existsByUserId(userId))
                                .thenReturn(true);

                registrationService.handleUserRegistered(event);

                verify(driverRepository, never()).save(any());

                verify(processedEventRepository).save(argThat(processed -> processed.getEventId().equals(eventId)));
        }
}
