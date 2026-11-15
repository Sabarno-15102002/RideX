package com.ridex.trip.event;

import com.ridex.trip.entity.OutboxEvent;
import com.ridex.trip.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {
    @Mock
    OutboxEventRepository repository;
    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher publisher() {
        return new OutboxPublisher(repository, kafkaTemplate);
    }

    private OutboxEvent event(String type) {
        return OutboxEvent.builder().id(UUID.randomUUID()).aggregateType("TRIP")
                .aggregateId(UUID.randomUUID()).eventType(type).payload("{payload}")
                .createdAt(Instant.now()).build();
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, String>> successFuture() {
        return CompletableFuture.completedFuture(mock(SendResult.class));
    }

    @Test
    void publishEvents_shouldPublishRequestedEventAndMarkItPublished() {
        OutboxEvent event = event("TRIP_REQUESTED");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send("trip.requested", event.getAggregateId().toString(), "{payload}"))
                .thenReturn(successFuture());

        publisher().publishEvents();

        assertNotNull(event.getPublishedAt());
        verify(kafkaTemplate).send("trip.requested", event.getAggregateId().toString(), "{payload}");
        verify(repository).save(event);
    }

    @Test
    void publishEvents_shouldRouteRematchingEventToRematchingTopic() {
        OutboxEvent event = event("TRIP_REMATCHING");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send("trip.rematching", event.getAggregateId().toString(), "{payload}"))
                .thenReturn(successFuture());

        publisher().publishEvents();

        verify(kafkaTemplate).send("trip.rematching", event.getAggregateId().toString(), "{payload}");
        verify(repository).save(event);
    }

    @Test
    void publishEvents_shouldRouteCompletedEventToCompletedTopic() {
        OutboxEvent event = event("TRIP_COMPLETED");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send("trip.completed", event.getAggregateId().toString(), "{payload}"))
                .thenReturn(successFuture());

        publisher().publishEvents();

        verify(kafkaTemplate).send("trip.completed", event.getAggregateId().toString(), "{payload}");
        verify(repository).save(event);
    }

    @Test
    void publishEvents_shouldNotMarkEventPublishedWhenKafkaFutureFails() {
        OutboxEvent event = event("TRIP_REQUESTED");
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failed);

        publisher().publishEvents();

        assertNull(event.getPublishedAt());
        verify(repository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void publishEvents_shouldHandleSynchronousKafkaSendFailure() {
        OutboxEvent event = event("TRIP_REQUESTED");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("producer unavailable"));

        assertDoesNotThrow(() -> publisher().publishEvents());
        assertNull(event.getPublishedAt());
        verify(repository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void publishEvents_shouldDoNothingWhenNoUnpublishedEventsExist() {
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of());

        publisher().publishEvents();

        verifyNoInteractions(kafkaTemplate);
        verify(repository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void publishEvents_shouldRejectUnsupportedEventType() {
        OutboxEvent event = event("UNKNOWN_EVENT");

        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        assertThrows(
                IllegalArgumentException.class,
                () -> publisher().publishEvents());

        verifyNoInteractions(kafkaTemplate);
        verify(repository, never()).save(any(OutboxEvent.class));

        assertNull(event.getPublishedAt());
    }

}
