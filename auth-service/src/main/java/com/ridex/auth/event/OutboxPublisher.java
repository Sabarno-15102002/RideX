package com.ridex.auth.event;

import java.time.Instant;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ridex.auth.entity.OutboxEvent;
import com.ridex.auth.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final String USER_REGISTERED_TOPIC = "user.registered";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

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
                            USER_REGISTERED_TOPIC,
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