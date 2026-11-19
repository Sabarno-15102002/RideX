package com.ridex.pricing.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import static org.junit.jupiter.api.Assertions.*;

class KafkaProducerConfigTest {

    private KafkaProducerConfig config;

    @BeforeEach
    void setUp() {
        config = new KafkaProducerConfig();

        org.springframework.test.util.ReflectionTestUtils.setField(
                config,
                "bootstrapServers",
                "localhost:9092");
    }

    @Test
    void shouldCreateOutboxProducerFactory() {
        ProducerFactory<String, String> factory = config.outboxProducerFactory();

        assertNotNull(factory);
    }

    @Test
    void shouldCreateOutboxKafkaTemplate() {
        ProducerFactory<String, String> factory = config.outboxProducerFactory();

        KafkaTemplate<String, String> template = config.outboxKafkaTemplate(factory);

        assertNotNull(template);
    }

    @Test
    void shouldCreateDeadLetterProducerFactory() {
        ProducerFactory<String, Object> factory = config.dltProducerFactory();

        assertNotNull(factory);
    }

    @Test
    void shouldCreateDeadLetterKafkaTemplate() {
        ProducerFactory<String, Object> factory = config.dltProducerFactory();

        KafkaTemplate<String, Object> template = config.dltKafkaTemplate(factory);

        assertNotNull(template);
    }
}
