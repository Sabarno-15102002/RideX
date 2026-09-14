package com.ridex.matching.event;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ridex.matching.entity.OutboxEvent;
import com.ridex.matching.repository.OutboxEventRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OutboxPublisher {

    private static final String DRIVER_MATCH_REQUESTED_TOPIC = "driver.match.requested";

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

        try {
            kafkaTemplate
                    .send(
                            DRIVER_MATCH_REQUESTED_TOPIC,
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