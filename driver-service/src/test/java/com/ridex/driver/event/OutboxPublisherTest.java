package com.ridex.driver.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.ridex.driver.entity.OutboxEvent;
import com.ridex.driver.repository.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

        @Mock
        private OutboxEventRepository repository;

        @Mock
        private KafkaTemplate<String, String> kafkaTemplate;

        @InjectMocks
        private OutboxPublisher publisher;

        @BeforeEach
        void setUp() {
                when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of());
        }

        @Test
        void publishEvents_shouldDoNothingWhenNoEventsExist() {
                publisher.publishEvents();

                verify(repository)
                                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

                verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void publishEvents_shouldPublishStatusChangedEvent() {
                verifyPublished("DRIVER_STATUS_CHANGED", "driver.status.changed");
        }

        @Test
        void publishEvents_shouldPublishRideAcceptedEvent() {
                verifyPublished("DRIVER_RIDE_ACCEPTED", "driver.ride.accepted");
        }

        @Test
        void publishEvents_shouldPublishRideRejectedEvent() {
                verifyPublished("DRIVER_RIDE_REJECTED", "driver.ride.rejected");
        }

        @Test
        void publishEvents_shouldPublishRideExpiredEvent() {
                verifyPublished("DRIVER_RIDE_EXPIRED", "driver.ride.expired");
        }

        @Test
        void publishEvents_shouldPublishArrivedEvent() {
                verifyPublished("DRIVER_ARRIVED", "driver.arrived");
        }

        @Test
        void publishEvents_shouldPublishTripStartedEvent() {
                verifyPublished("DRIVER_TRIP_STARTED", "driver.trip.started");
        }

        @Test
        void publishEvents_shouldPublishTripCompletedEvent() {
                verifyPublished("DRIVER_TRIP_COMPLETED", "driver.trip.completed");
        }

        @Test
        void publishEvents_shouldMarkEventPublishedAfterSuccessfulSend() {
                OutboxEvent event = createEvent(
                                "DRIVER_RIDE_ACCEPTED",
                                UUID.randomUUID());

                when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of(event));

                when(kafkaTemplate.send(
                                eq("driver.ride.accepted"),
                                eq(event.getAggregateId().toString()),
                                eq(event.getPayload()))).thenReturn(CompletableFuture.completedFuture(null));

                publisher.publishEvents();

                assertNotNull(event.getPublishedAt());
                verify(repository).save(event);
        }

        @Test
        void publishEvents_shouldNotMarkEventPublishedWhenAsyncSendFails() {
                OutboxEvent event = createEvent(
                                "DRIVER_RIDE_ACCEPTED",
                                UUID.randomUUID());

                when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of(event));

                when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                                .thenReturn(CompletableFuture.failedFuture(
                                                new RuntimeException("Kafka unavailable")));

                publisher.publishEvents();

                assertNull(event.getPublishedAt());
                verify(repository, never()).save(event);
        }

        @Test
        void publishEvents_shouldCatchSynchronousSendException() {
                OutboxEvent event = createEvent(
                                "DRIVER_RIDE_ACCEPTED",
                                UUID.randomUUID());

                when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of(event));

                when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                                .thenThrow(new RuntimeException("Kafka unavailable"));

                assertDoesNotThrow(() -> publisher.publishEvents());

                assertNull(event.getPublishedAt());
                verify(repository, never()).save(event);
        }

        @Test
        void publishEvents_shouldProcessMultipleEvents() {
                OutboxEvent first = createEvent(
                                "DRIVER_RIDE_ACCEPTED",
                                UUID.randomUUID());

                OutboxEvent second = createEvent(
                                "DRIVER_TRIP_COMPLETED",
                                UUID.randomUUID());

                when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of(first, second));

                when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                                .thenReturn(CompletableFuture.completedFuture(null));

                publisher.publishEvents();

                assertNotNull(first.getPublishedAt());
                assertNotNull(second.getPublishedAt());

                verify(repository).save(first);
                verify(repository).save(second);
        }

        private void verifyPublished(String eventType, String expectedTopic) {
                UUID driverId = UUID.randomUUID();
                OutboxEvent event = createEvent(eventType, driverId);

                when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of(event));

                when(kafkaTemplate.send(
                                eq(expectedTopic),
                                eq(driverId.toString()),
                                eq(event.getPayload()))).thenReturn(CompletableFuture.completedFuture(null));

                publisher.publishEvents();

                verify(kafkaTemplate).send(
                                expectedTopic,
                                driverId.toString(),
                                event.getPayload());

                assertNotNull(event.getPublishedAt());
                verify(repository).save(event);
        }

        @Test
        void publishEvents_shouldUseAcceptedTopicForUnknownEventType() {
                UUID driverId = UUID.randomUUID();

                OutboxEvent event = createEvent(
                                "UNKNOWN_EVENT",
                                driverId);

                when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                                .thenReturn(List.of(event));

                when(kafkaTemplate.send(
                                eq("driver.ride.accepted"),
                                eq(driverId.toString()),
                                eq(event.getPayload()))).thenReturn(CompletableFuture.completedFuture(null));

                publisher.publishEvents();

                verify(kafkaTemplate).send(
                                "driver.ride.accepted",
                                driverId.toString(),
                                event.getPayload());

                assertNotNull(event.getPublishedAt());
                verify(repository).save(event);
        }

        private OutboxEvent createEvent(String eventType, UUID driverId) {
                return OutboxEvent.builder()
                                .id(UUID.randomUUID())
                                .aggregateType("DRIVER")
                                .aggregateId(driverId)
                                .eventType(eventType)
                                .payload("{\"event\":\"test\"}")
                                .createdAt(Instant.now())
                                .build();
        }
}
