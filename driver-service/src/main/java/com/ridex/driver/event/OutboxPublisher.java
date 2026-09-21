package com.ridex.driver.event;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ridex.driver.entity.OutboxEvent;
import com.ridex.driver.repository.OutboxEventRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OutboxPublisher {

    private static final String DRIVER_STATUS_CHANGED_TOPIC = "driver.status.changed";
    private static final String DRIVER_RIDE_ACCEPTED_TOPIC = "driver.ride.accepted";
    private static final String DRIVER_RIDE_REJECTED_TOPIC = "driver.ride.rejected";
    private static final String DRIVER_RIDE_EXPIRED_TOPIC = "driver.ride.expired";
    private static final String DRIVER_RIDE_ARRIVED_TOPIC = "driver.arrived";
    private static final String DRIVER_RIDE_STARTED_TOPIC = "driver.trip.started";
    private static final String DRIVER_RIDE_COMPLETED_TOPIC = "driver.trip.completed";

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
            case "DRIVER_STATUS_CHANGED":
                topic = DRIVER_STATUS_CHANGED_TOPIC;
                break;

            case "DRIVER_RIDE_ACCEPTED":
                topic = DRIVER_RIDE_ACCEPTED_TOPIC;
                break;
            
            case "DRIVER_RIDE_REJECTED":
                topic = DRIVER_RIDE_REJECTED_TOPIC;
                break;

            case "DRIVER_RIDE_EXPIRED":
                topic = DRIVER_RIDE_EXPIRED_TOPIC;
                break;
            
            case "DRIVER_ARRIVED":
                topic = DRIVER_RIDE_ARRIVED_TOPIC;
                break;
            
            case "DRIVER_TRIP_STARTED":
                topic = DRIVER_RIDE_STARTED_TOPIC;
                break;

            case "DRIVER_TRIP_COMPLETED":
                topic = DRIVER_RIDE_COMPLETED_TOPIC;
                break;
        
            default:
                topic = DRIVER_RIDE_ACCEPTED_TOPIC;
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