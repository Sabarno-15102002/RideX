package com.ridex.driver.event;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class KafkaDltReplayOperationsIntegrationTest {

        private static final String DLT_TOPIC = "ridex.dlt.test.DLT";
        private static final String TARGET_TOPIC = "ridex.dlt.test";

        @Container
        static final KafkaContainer KAFKA = new KafkaContainer(
                        DockerImageName.parse("apache/kafka-native:3.8.0"));

        @BeforeAll
        static void createTopics() throws Exception {
                Map<String, Object> config = Map.of(
                                "bootstrap.servers", KAFKA.getBootstrapServers());

                try (AdminClient admin = AdminClient.create(config)) {
                        admin.createTopics(List.of(
                                        new NewTopic(DLT_TOPIC, 1, (short) 1),
                                        new NewTopic(TARGET_TOPIC, 1, (short) 1))).all().get();
                }
        }

        @Test
        void replay_shouldPublishMatchingDltRecordToTargetTopic()
                        throws Exception {

                String testId = UUID.randomUUID().toString();
                String payload = """
                                {"event":"driver-status-changed","status":"AVAILABLE"}
                                """.trim();

                publishDltRecord(testId, payload);

                KafkaDltReplayOperations operations = new KafkaDltReplayOperations(
                                KAFKA.getBootstrapServers(),
                                Duration.ofMillis(1500));

                operations.replay(testId);

                ConsumerRecord<String, String> replayedRecord = consumeTargetRecord(testId);

                assertNotNull(replayedRecord);
                assertEquals(testId, replayedRecord.key());
                assertEquals(payload, replayedRecord.value());
                assertEquals(TARGET_TOPIC, replayedRecord.topic());
        }

        @Test
        void replay_whenNoMatchingRecordExists_shouldThrowIllegalStateException()
                        throws Exception {

                String existingKey = UUID.randomUUID().toString();
                String requestedTestId = UUID.randomUUID().toString();

                publishDltRecord(existingKey, "{\"event\":\"unrelated\"}");

                KafkaDltReplayOperations operations = new KafkaDltReplayOperations(
                                KAFKA.getBootstrapServers(),
                                Duration.ofMillis(1500));

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> operations.replay(requestedTestId));

                assertEquals(
                                "No DLT record found for test ID: " + requestedTestId,
                                exception.getMessage());
        }

        private void publishDltRecord(String key, String value)
                        throws Exception {

                Map<String, Object> properties = new HashMap<>();
                properties.put(
                                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                KAFKA.getBootstrapServers());
                properties.put(
                                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                                StringSerializer.class);
                properties.put(
                                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                StringSerializer.class);

                try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {

                        producer.send(
                                        new ProducerRecord<>(DLT_TOPIC, key, value)).get();
                }
        }

        private ConsumerRecord<String, String> consumeTargetRecord(
                        String expectedKey) {

                Map<String, Object> properties = new HashMap<>();
                properties.put(
                                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                KAFKA.getBootstrapServers());
                properties.put(
                                ConsumerConfig.GROUP_ID_CONFIG,
                                "ridex-replay-test-" + UUID.randomUUID());
                properties.put(
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                                "earliest");
                properties.put(
                                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                StringDeserializer.class);
                properties.put(
                                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                StringDeserializer.class);

                try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {

                        consumer.subscribe(List.of(TARGET_TOPIC));

                        long deadline = System.nanoTime()
                                        + Duration.ofSeconds(15).toNanos();

                        while (System.nanoTime() < deadline) {
                                var records = consumer.poll(Duration.ofMillis(500));

                                for (ConsumerRecord<String, String> rec : records) {
                                        if (expectedKey.equals(rec.key())) {
                                                return rec;
                                        }
                                }
                        }
                }

                return null;
        }
}
