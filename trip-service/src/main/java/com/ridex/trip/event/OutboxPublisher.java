package com.ridex.trip.event;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ridex.trip.entity.OutboxEvent;
import com.ridex.trip.repository.OutboxEventRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OutboxPublisher {

    private static final String TRIP_REQUESTED_TOPIC = "trip.requested";
    private static final String TRIP_REMATCHING_TOPIC = "trip.rematching";

    private final OutboxEventRepository outboxEventRepository;

    @Qualifier("outboxKafkaTemplate")
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            @Qualifier("outboxKafkaTemplate")
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    public void publishEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {

        String topic;
        String eventType = event.getEventType();
        switch (eventType) {
            case "TRIP_REQUESTED":
                topic = TRIP_REQUESTED_TOPIC;
                break;

            case "TRIP_REMATCHING":
                topic = TRIP_REMATCHING_TOPIC;
                break;
        
            default:
                topic = TRIP_REQUESTED_TOPIC;
                break;
        }

        try {
            kafkaTemplate
                    .send(
                            topic,
                            event.getAggregateId().toString(),
                            event.getPayload()
                    )
                    .whenComplete((result, exception) -> {

                        if (exception != null) {
                            log.error(
                                    "Failed to publish outbox event: {}",
                                    event.getId(),
                                    exception
                            );
                            return;
                        }

                        markAsPublished(event);
                    });

        } catch (Exception e) {
            log.error(
                    "Failed to send outbox event: {}",
                    event.getId(),
                    e
            );
        }
    }

    private void markAsPublished(OutboxEvent event) {

        event.setPublishedAt(Instant.now());

        outboxEventRepository.save(event);

        log.info(
                "Published outbox event: {}",
                event.getId()
        );
    }
}