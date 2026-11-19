package com.ridex.pricing.config;

import com.ridex.pricing.event.RiderCreatedEvent;
import com.ridex.pricing.event.TripCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaConsumerConfigTest {

    private KafkaConsumerConfig config;
    private DefaultErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        config = new KafkaConsumerConfig();
        errorHandler = mock(DefaultErrorHandler.class);

        org.springframework.test.util.ReflectionTestUtils.setField(
                config,
                "bootstrapServers",
                "localhost:9092"
        );
    }

    @Test
    void shouldCreateTripCompletedConsumerFactory() {
        ConsumerFactory<String, TripCompletedEvent> factory =
                config.tripCompletedConsumerFactory();

        assertNotNull(factory);
    }

    @Test
    void shouldCreateRiderCreatedConsumerFactory() {
        ConsumerFactory<String, RiderCreatedEvent> factory =
                config.riderCreatedConsumerFactory();

        assertNotNull(factory);
    }

    @Test
    void shouldCreateTripCompletedListenerFactory() {
        ConsumerFactory<String, TripCompletedEvent> consumerFactory =
                config.tripCompletedConsumerFactory();

        ConcurrentKafkaListenerContainerFactory<String, TripCompletedEvent>
                factory = config.tripCompletedKafkaListenerContainerFactory(
                        consumerFactory,
                        errorHandler
                );

        assertNotNull(factory);
        assertTrue(factory.getContainerProperties().isObservationEnabled());
    }

    @Test
    void shouldCreateRiderCreatedListenerFactory() {
        ConsumerFactory<String, RiderCreatedEvent> consumerFactory =
                config.riderCreatedConsumerFactory();

        ConcurrentKafkaListenerContainerFactory<String, RiderCreatedEvent>
                factory = config.riderCreatedKafkaListenerContainerFactory(
                        consumerFactory,
                        errorHandler
                );

        assertNotNull(factory);
        assertTrue(factory.getContainerProperties().isObservationEnabled());
    }
}
