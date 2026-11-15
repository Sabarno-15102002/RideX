package com.ridex.auth.event;

import com.ridex.auth.entity.OutboxEvent;
import com.ridex.auth.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxPublisher publisher;

    private OutboxEvent createEvent() {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("USER")
                .aggregateId(UUID.randomUUID())
                .eventType("USER_REGISTERED")
                .payload("{\"name\":\"Test Rider\"}")
                .build();
    }

    @Test
    void publishEvents_shouldDoNothingWhenNoEventsExist() {
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of());

        publisher.publishEvents();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publishEvents_shouldSendPendingEvents() {
        OutboxEvent event = createEvent();

        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(
                "user.registered",
                event.getAggregateId().toString(),
                event.getPayload()))
                .thenReturn(new CompletableFuture<>());

        publisher.publishEvents();

        verify(kafkaTemplate).send(
                "user.registered",
                event.getAggregateId().toString(),
                event.getPayload());
    }

    @Test
    void publishEvents_shouldMarkEventPublishedAfterSuccessfulSend() {
        OutboxEvent event = createEvent();

        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(
                "user.registered",
                event.getAggregateId().toString(),
                event.getPayload()))
                .thenReturn(future);

        publisher.publishEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

        verify(repository, timeout(1000)).save(captor.capture());

        assertNotNull(captor.getValue().getPublishedAt());
    }

    @Test
    void publishEvents_shouldNotMarkEventPublishedWhenSendFails() {
        OutboxEvent event = createEvent();

        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();

        future.completeExceptionally(new RuntimeException("Kafka unavailable"));

        when(kafkaTemplate.send(
                "user.registered",
                event.getAggregateId().toString(),
                event.getPayload()))
                .thenReturn(future);

        publisher.publishEvents();

        verify(repository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void publishEvents_shouldHandleSynchronousSendFailure() {
        OutboxEvent event = createEvent();

        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(
                "user.registered",
                event.getAggregateId().toString(),
                event.getPayload()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        assertDoesNotThrow(() -> publisher.publishEvents());

        verify(repository, never()).save(any(OutboxEvent.class));
    }
}
