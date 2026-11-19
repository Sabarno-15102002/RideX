package com.ridex.payment.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;

class KafkaErrorHandlerConfigTest {

    private final KafkaErrorHandlerConfig config = new KafkaErrorHandlerConfig();

    @Test
    void shouldCreateDeadLetterRecoverer() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

        DeadLetterPublishingRecoverer recoverer = config.deadLetterPublishingRecoverer(kafkaTemplate);

        assertNotNull(recoverer);
    }

    @Test
    void shouldPublishFailedRecordToDeadLetterTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

        when(kafkaTemplate.send(
                org.mockito.ArgumentMatchers
                        .<ProducerRecord<String, Object>>any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        DeadLetterPublishingRecoverer recoverer = config.deadLetterPublishingRecoverer(kafkaTemplate);

        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                "payment-events",
                3,
                10L,
                "key",
                "payload");

        recoverer.accept(record, new RuntimeException("Consumer failed"));

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);

        verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, Object> sentRecord = captor.getValue();

        assertEquals("payment-events.DLT", sentRecord.topic());
        assertEquals(3, sentRecord.partition());
        assertEquals("key", sentRecord.key());
        assertEquals("payload", sentRecord.value());

    }

    @Test
    void shouldCreateDefaultErrorHandler() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

        DeadLetterPublishingRecoverer recoverer = config.deadLetterPublishingRecoverer(kafkaTemplate);

        DefaultErrorHandler errorHandler = config.kafkaErrorHandler(recoverer);

        assertNotNull(errorHandler);
    }
}
