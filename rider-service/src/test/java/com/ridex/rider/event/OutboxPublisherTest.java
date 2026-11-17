
package com.ridex.rider.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.ridex.rider.entity.OutboxEvent;
import com.ridex.rider.repository.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("RIDER")
                .aggregateId(UUID.randomUUID())
                .eventType("RIDER_CREATED")
                .payload("{\"eventType\":\"RIDER_CREATED\"}")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void publishEvents_shouldSendEventAndMarkItPublished() {
        when(outboxEventRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(
                "rider.created",
                event.getAggregateId().toString(),
                event.getPayload()))
                .thenReturn(future);

        outboxPublisher.publishEvents();

        assertNotNull(event.getPublishedAt());

        verify(kafkaTemplate).send(
                "rider.created",
                event.getAggregateId().toString(),
                event.getPayload());

        verify(outboxEventRepository).save(event);
    }

    @Test
    void publishEvents_shouldNotSendAnythingWhenNoUnpublishedEventsExist() {
        when(outboxEventRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of());

        outboxPublisher.publishEvents();

        verifyNoInteractions(kafkaTemplate);
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void publishEvents_shouldNotMarkEventPublishedWhenKafkaSendFails() {
        when(outboxEventRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.failedFuture(
                new RuntimeException("Kafka unavailable"));

        when(kafkaTemplate.send(
                "rider.created",
                event.getAggregateId().toString(),
                event.getPayload()))
                .thenReturn(future);

        outboxPublisher.publishEvents();

        assertNull(event.getPublishedAt());

        verify(kafkaTemplate).send(
                "rider.created",
                event.getAggregateId().toString(),
                event.getPayload());

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void publishEvents_shouldCatchSynchronousKafkaSendException() {
        when(outboxEventRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(
                "rider.created",
                event.getAggregateId().toString(),
                event.getPayload()))
                .thenThrow(new RuntimeException("Kafka send failed"));

        assertDoesNotThrow(() -> outboxPublisher.publishEvents());

        assertNull(event.getPublishedAt());

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void publishEvents_shouldProcessMultipleEvents() {
        OutboxEvent secondEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("RIDER")
                .aggregateId(UUID.randomUUID())
                .eventType("RIDER_CREATED")
                .payload("{\"eventType\":\"RIDER_CREATED_2\"}")
                .createdAt(Instant.now())
                .build();

        when(outboxEventRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event, secondEvent));

        when(kafkaTemplate.send(
                eq("rider.created"),
                any(String.class),
                any(String.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(null));

        outboxPublisher.publishEvents();

        assertNotNull(event.getPublishedAt());
        assertNotNull(secondEvent.getPublishedAt());

        verify(kafkaTemplate, times(2)).send(
                eq("rider.created"),
                any(String.class),
                any(String.class));

        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }
}
