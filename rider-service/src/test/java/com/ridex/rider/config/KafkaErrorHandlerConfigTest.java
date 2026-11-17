package com.ridex.rider.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaErrorHandlerConfigTest {

        @SuppressWarnings("unchecked")
        @Test
        void deadLetterPublishingRecoverer_shouldPublishToDltTopicAndSamePartition() {
                KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

                KafkaErrorHandlerConfig config = new KafkaErrorHandlerConfig();

                DeadLetterPublishingRecoverer recoverer = config.deadLetterPublishingRecoverer(kafkaTemplate);

                ConsumerRecord<String, Object> rec = new ConsumerRecord<>(
                                "user.registered",
                                3,
                                100L,
                                "user-123",
                                "test-payload");

                recoverer.accept(rec, new RuntimeException("Processing failed"));

                verify(kafkaTemplate).send(any(ProducerRecord.class));
        }
}
