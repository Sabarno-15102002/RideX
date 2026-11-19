package com.ridex.pricing.config;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaErrorHandlerConfigTest {

    private final KafkaErrorHandlerConfig config =
            new KafkaErrorHandlerConfig();

    @Test
    void shouldCreateDeadLetterRecoverer() {
        KafkaTemplate<String, Object> template =
                mock(KafkaTemplate.class);

        DeadLetterPublishingRecoverer recoverer =
                config.deadLetterPublishingRecoverer(template);

        assertNotNull(recoverer);
    }

    @Test
    void shouldCreateDefaultErrorHandler() {
        KafkaTemplate<String, Object> template =
                mock(KafkaTemplate.class);

        DeadLetterPublishingRecoverer recoverer =
                config.deadLetterPublishingRecoverer(template);

        DefaultErrorHandler errorHandler =
                config.kafkaErrorHandler(recoverer);

        assertNotNull(errorHandler);
    }
}
