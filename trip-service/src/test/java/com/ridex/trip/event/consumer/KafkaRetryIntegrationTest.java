package com.ridex.trip.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridex.trip.config.KafkaConsumerConfig;
import com.ridex.trip.config.KafkaErrorHandlerConfig;
import com.ridex.trip.config.KafkaProducerConfig;
import com.ridex.trip.event.event.DriverMatchRequestedEvent;
import com.ridex.trip.service.TripService;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = KafkaRetryIntegrationTest.KafkaTestConfig.class)
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class KafkaRetryIntegrationTest {

        private static final String SOURCE_TOPIC = "driver.match.requested";

        private static final String DLT_TOPIC = SOURCE_TOPIC + ".DLT";

        @Container
        static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.8.0");

        @DynamicPropertySource
        static void configureKafka(DynamicPropertyRegistry registry) {
                registry.add(
                                "spring.kafka.bootstrap-servers",
                                KAFKA::getBootstrapServers);
        }

        @Configuration
        @EnableKafka
        @Import({
                        KafkaConsumerConfig.class,
                        KafkaProducerConfig.class,
                        KafkaErrorHandlerConfig.class,
                        DriverMatchRequestedConsumer.class
        })
        static class KafkaTestConfig {
        }

        @MockitoBean
        private TripService tripService;

        @Autowired
        private KafkaTemplate<String, String> outboxKafkaTemplate;

        private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        @BeforeAll
        static void createTopics() throws Exception {

                Properties properties = new Properties();

                properties.put(
                                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                                KAFKA.getBootstrapServers());

                try (AdminClient adminClient = AdminClient.create(properties)) {

                        adminClient.createTopics(List.of(
                                        new NewTopic(SOURCE_TOPIC, 1, (short) 1),
                                        new NewTopic(DLT_TOPIC, 1, (short) 1))).all().get(15, TimeUnit.SECONDS);
                }
        }

        @BeforeEach
        void resetMocks() {
                Mockito.reset(tripService);
        }

        private DriverMatchRequestedEvent createEvent() {
                return new DriverMatchRequestedEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                Instant.now());
        }

        private void publishEvent(
                        DriverMatchRequestedEvent event) throws Exception {

                String payload = objectMapper.writeValueAsString(event);

                outboxKafkaTemplate.send(
                                SOURCE_TOPIC,
                                event.eventId().toString(),
                                payload).get(10, TimeUnit.SECONDS);
        }

        private KafkaConsumer<String, String> createDltConsumer() {

                Properties properties = new Properties();

                properties.put(
                                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                KAFKA.getBootstrapServers());

                properties.put(
                                ConsumerConfig.GROUP_ID_CONFIG,
                                "dlt-test-" + UUID.randomUUID());

                properties.put(
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                                "latest");

                properties.put(
                                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                                false);

                properties.put(
                                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                StringDeserializer.class);

                properties.put(
                                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                StringDeserializer.class);

                KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

                consumer.subscribe(List.of(DLT_TOPIC));

                // Wait until the consumer is assigned the DLT partition.
                long deadline = System.nanoTime()
                                + TimeUnit.SECONDS.toNanos(10);

                while (consumer.assignment().isEmpty()
                                && System.nanoTime() < deadline) {

                        consumer.poll(Duration.ofMillis(100));
                }

                assertFalse(
                                consumer.assignment().isEmpty(),
                                "DLT consumer should receive a partition assignment");

                return consumer;
        }

        private ConsumerRecord<String, String> pollDltRecord(
                        KafkaConsumer<String, String> consumer) {

                long deadline = System.nanoTime()
                                + TimeUnit.SECONDS.toNanos(20);

                while (System.nanoTime() < deadline) {

                        var records = consumer.poll(Duration.ofMillis(500));

                        for (ConsumerRecord<String, String> rec : records) {
                                return rec;
                        }
                }

                fail("No record received from DLT within timeout");
                return null;
        }

        // ---------------------------------------------------------
        // TEST 1: Successful message processing
        // ---------------------------------------------------------

        @Test
        void successfulMessage_shouldBeProcessedOnce()
                        throws Exception {

                DriverMatchRequestedEvent event = createEvent();

                publishEvent(event);

                verify(tripService, timeout(10000).times(1))
                                .assignDriver(any(DriverMatchRequestedEvent.class));

                verifyNoMoreInteractions(tripService);
        }

        // ---------------------------------------------------------
        // TEST 2: Retry when consumer throws an exception
        // ---------------------------------------------------------

        @Test
        void failedMessage_shouldRetryAndEventuallySucceed()
                        throws Exception {

                DriverMatchRequestedEvent event = createEvent();

                AtomicInteger attempts = new AtomicInteger();

                doAnswer(invocation -> {

                        int currentAttempt = attempts.incrementAndGet();

                        if (currentAttempt <= 2) {
                                throw new IllegalStateException(
                                                "Simulated temporary failure");
                        }

                        return null;

                }).when(tripService)
                                .assignDriver(any(DriverMatchRequestedEvent.class));

                publishEvent(event);

                verify(tripService, timeout(15000).times(3))
                                .assignDriver(any(DriverMatchRequestedEvent.class));

                assertEquals(3, attempts.get());

                verifyNoMoreInteractions(tripService);
        }

        // ---------------------------------------------------------
        // TEST 3: Retries exhausted -> Dead Letter Topic
        // ---------------------------------------------------------

        @Test
        void failedMessageAfterRetries_shouldReachDeadLetterTopic()
                        throws Exception {

                DriverMatchRequestedEvent event = createEvent();

                AtomicInteger attempts = new AtomicInteger();

                doAnswer(invocation -> {

                        attempts.incrementAndGet();

                        throw new IllegalStateException(
                                        "Simulated permanent failure");

                }).when(tripService)
                                .assignDriver(any(DriverMatchRequestedEvent.class));

                try (KafkaConsumer<String, String> dltConsumer = createDltConsumer()) {

                        publishEvent(event);

                        // Initial attempt + two retries.
                        verify(tripService, timeout(15000).times(3))
                                        .assignDriver(
                                                        any(DriverMatchRequestedEvent.class));

                        assertEquals(3, attempts.get());

                        ConsumerRecord<String, String> dltRecord = pollDltRecord(dltConsumer);

                        assertNotNull(dltRecord);

                        assertEquals(DLT_TOPIC, dltRecord.topic());

                        assertTrue(
                                        dltRecord.value().contains(
                                                        event.eventId().toString()),
                                        "DLT payload should contain the original event");

                        assertNotNull(
                                        dltRecord.headers().lastHeader(
                                                        KafkaHeaders.DLT_EXCEPTION_FQCN),
                                        "DLT should contain the original exception class");

                        assertNotNull(
                                        dltRecord.headers().lastHeader(
                                                        KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                                        "DLT should contain the exception message");

                        var originalTopicHeader = dltRecord.headers().lastHeader(
                                        KafkaHeaders.DLT_ORIGINAL_TOPIC);

                        assertNotNull(originalTopicHeader);

                        assertEquals(
                                        SOURCE_TOPIC,
                                        new String(
                                                        originalTopicHeader.value(),
                                                        StandardCharsets.UTF_8));
                }
        }
}
