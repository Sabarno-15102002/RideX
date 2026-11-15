package com.ridex.auth.service.util;

import com.ridex.auth.entity.OutboxEvent;
import com.ridex.auth.event.EventSerializer;
import com.ridex.auth.event.UserRegisteredEvent;
import com.ridex.auth.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

        @Mock
        private OutboxEventRepository repository;

        @Mock
        private EventSerializer serializer;

        @InjectMocks
        private OutboxEventService service;

        @Test
        void saveUserRegisteredEvent_shouldCreateOutboxRecord() {
                UUID userId = UUID.randomUUID();

                UserRegisteredEvent event = new UserRegisteredEvent(
                                UUID.randomUUID(),
                                userId,
                                "Test Rider",
                                "RIDER",
                                Instant.now());

                String expectedPayload = "{\"userId\":\"" + userId + "\"}";

                when(serializer.serialize(event)).thenReturn(expectedPayload);

                service.saveUserRegisteredEvent(event);

                ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

                verify(repository).save(captor.capture());

                OutboxEvent saved = captor.getValue();

                assertEquals("USER", saved.getAggregateType());
                assertEquals(userId, saved.getAggregateId());
                assertEquals("USER_REGISTERED", saved.getEventType());
                assertEquals(expectedPayload, saved.getPayload());

                verify(serializer, times(1)).serialize(event);
        }

        @Test
        void saveUserRegisteredEvent_shouldPropagateSerializationFailure() {
                UserRegisteredEvent event = new UserRegisteredEvent(
                                UUID.randomUUID(), UUID.randomUUID(),
                                "Test Rider", "RIDER", Instant.now());

                when(serializer.serialize(event))
                                .thenThrow(new RuntimeException("Serialization failed"));

                assertThrows(RuntimeException.class,
                                () -> service.saveUserRegisteredEvent(event));

                verify(repository, never()).save(any());
        }
}
