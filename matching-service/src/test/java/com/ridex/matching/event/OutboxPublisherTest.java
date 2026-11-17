
package com.ridex.matching.event;

import com.ridex.matching.entity.OutboxEvent;
import com.ridex.matching.repository.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

        private static final String TOPIC = "driver.match.requested";

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
                                .aggregateType("DRIVER_MATCH")
                                .aggregateId(UUID.randomUUID())
                                .eventType("DRIVER_MATCH_REQUESTED")
                                .payload("{\"eventId\":\"test\"}")
                                .createdAt(Instant.now())
                                .build();
        }

        @Test
        void publishEvents_shouldLoadUnpublishedEvents() {
                when(outboxEventRepository
                                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of());

                outboxPublisher.publishEvents();

                verify(outboxEventRepository)
                                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

                verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void publishEvents_shouldPublishEventAndMarkItAsPublished() {
                when(outboxEventRepository
                                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of(event));

                when(kafkaTemplate.send(
                                anyString(),
                                anyString(),
                                anyString())).thenReturn(
                                                CompletableFuture.<SendResult<String, String>>completedFuture(null));

                outboxPublisher.publishEvents();

                verify(kafkaTemplate).send(
                                TOPIC,
                                event.getAggregateId().toString(),
                                event.getPayload());

                assertThat(event.getPublishedAt()).isNotNull();

                verify(outboxEventRepository).save(event);
        }

        @Test
        void publishEvents_shouldNotMarkEventPublishedWhenKafkaSendFails() {
                when(outboxEventRepository
                                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of(event));

                CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();

                failedFuture.completeExceptionally(
                                new RuntimeException("Kafka unavailable"));

                when(kafkaTemplate.send(
                                anyString(),
                                anyString(),
                                anyString())).thenReturn(failedFuture);

                outboxPublisher.publishEvents();

                assertThat(event.getPublishedAt()).isNull();

                verify(outboxEventRepository, never()).save(any());
        }

        @Test
        void publishEvents_shouldHandleSynchronousKafkaException() {
                when(outboxEventRepository
                                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of(event));

                when(kafkaTemplate.send(
                                anyString(),
                                anyString(),
                                anyString())).thenThrow(new RuntimeException("Kafka unavailable"));

                outboxPublisher.publishEvents();

                assertThat(event.getPublishedAt()).isNull();

                verify(outboxEventRepository, never()).save(any());
        }

        @Test
        void publishEvents_shouldPublishMultipleEvents() {
                OutboxEvent secondEvent = OutboxEvent.builder()
                                .id(UUID.randomUUID())
                                .aggregateType("DRIVER_MATCH")
                                .aggregateId(UUID.randomUUID())
                                .eventType("DRIVER_MATCH_REQUESTED")
                                .payload("{\"eventId\":\"second\"}")
                                .createdAt(Instant.now())
                                .build();

                when(outboxEventRepository
                                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of(event, secondEvent));

                when(kafkaTemplate.send(
                                anyString(),
                                anyString(),
                                anyString())).thenReturn(
                                                CompletableFuture.<SendResult<String, String>>completedFuture(null));

                outboxPublisher.publishEvents();

                ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

                ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

                verify(kafkaTemplate, times(2)).send(
                                eq(TOPIC),
                                keyCaptor.capture(),
                                payloadCaptor.capture());

                assertThat(keyCaptor.getAllValues())
                                .containsExactly(
                                                event.getAggregateId().toString(),
                                                secondEvent.getAggregateId().toString());

                assertThat(payloadCaptor.getAllValues())
                                .containsExactly(
                                                event.getPayload(),
                                                secondEvent.getPayload());

                verify(outboxEventRepository).save(event);
                verify(outboxEventRepository).save(secondEvent);

                assertThat(event.getPublishedAt()).isNotNull();
                assertThat(secondEvent.getPublishedAt()).isNotNull();
        }
}
