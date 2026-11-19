package com.ridex.payment.event;

import com.ridex.payment.entity.OutboxEvent;
import com.ridex.payment.repository.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

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

    OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(repository, kafkaTemplate);
    }

    private OutboxEvent event(String type) {
        return OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(UUID.randomUUID())
                .eventType(type)
                .payload("{\"payment\":\"test\"}")
                .build();
    }

    @Test
    void publishesSucceededEventToCorrectTopic() {
        OutboxEvent event = event("PAYMENT_SUCCEEDED");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("payment.succeeded"), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishEvents();

        verify(kafkaTemplate).send(
                eq("payment.succeeded"),
                eq(event.getAggregateId().toString()),
                eq(event.getPayload()));
    }

    @Test
    void publishesFailedEventToCorrectTopic() {
        OutboxEvent event = event("PAYMENT_FAILED");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("payment.failed"), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishEvents();

        verify(kafkaTemplate).send(eq("payment.failed"), anyString(), anyString());
    }

    @Test
    void publishesRefundedEventToCorrectTopic() {
        OutboxEvent event = event("PAYMENT_REFUNDED");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("payment.refunded"), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishEvents();

        verify(kafkaTemplate).send(eq("payment.refunded"), anyString(), anyString());
    }

    @Test
    void publishesCreatedEventToCorrectTopic() {
        OutboxEvent event = event("PAYMENT_CREATED");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("payment.created"), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishEvents();

        verify(kafkaTemplate).send(eq("payment.created"), anyString(), anyString());
    }

    @Test
    void unknownEventTypeFallsBackToFailedTopic() {
        OutboxEvent event = event("UNKNOWN");
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("payment.failed"), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishEvents();

        verify(kafkaTemplate).send(eq("payment.failed"), anyString(), anyString());
    }

    @Test
    void emptyOutboxDoesNotPublish() {
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of());

        publisher.publishEvents();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void asynchronousFailureDoesNotMarkEventPublished() {
        OutboxEvent event = event("PAYMENT_CREATED");
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("broker unavailable"));

        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        publisher.publishEvents();

        verify(repository, never()).save(any());
    }

    @Test
    void synchronousSendFailureDoesNotMarkEventPublished() {
        OutboxEvent event = event("PAYMENT_CREATED");

        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("send failed"));

        publisher.publishEvents();

        verify(repository, never()).save(any());
    }

    @Test
    void successfulSendMarksEventPublished() {
        OutboxEvent event = event("PAYMENT_CREATED");
        CompletableFuture<SendResult<String, String>> future = CompletableFuture
                .completedFuture(mock(SendResult.class));

        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        publisher.publishEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository, timeout(1000)).save(captor.capture());

        assertNotNull(captor.getValue().getPublishedAt());
    }
}
