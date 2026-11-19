package com.ridex.location.config;

import com.ridex.location.event.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class KafkaConsumerConfigTest {
    @Test
    void createsDriverStatusConsumerWithExpectedType() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        ConsumerFactory<String, DriverStatusChangedEvent> factory = config.driverStatusChangedConsumerFactory();
        assertInstanceOf(DefaultKafkaConsumerFactory.class, factory);
        Map<String, Object> props = ((DefaultKafkaConsumerFactory<String, DriverStatusChangedEvent>) factory)
                .getConfigurationProperties();
        assertEquals("localhost:9092", props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(false, props.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
        assertEquals("earliest", props.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        assertEquals("com.ridex.location.event", props.get("spring.json.trusted.packages"));
        assertEquals(DriverStatusChangedEvent.class, props.get("spring.json.value.default.type"));
    }

    @Test
    void createsTripCompletedConsumerWithExpectedType() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "kafka:9092");
        var factory = (DefaultKafkaConsumerFactory<String, TripCompletedEvent>) config.tripCompletedConsumerFactory();
        assertEquals(TripCompletedEvent.class,
                factory.getConfigurationProperties().get("spring.json.value.default.type"));
    }
}
