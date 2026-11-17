package com.ridex.driver.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
public class KafkaDltReplayOperations implements DltReplayOperations {

        private static final String DLT_TOPIC = "ridex.dlt.test.DLT";
        private static final String TARGET_TOPIC = "ridex.dlt.test";

        private final String bootstrapServers;

        private final Duration pollingTimeout;

        public KafkaDltReplayOperations(
                @Value("${spring.kafka.bootstrap-servers:kafka:9092}")
                String bootstrapServers,
                @Value("${ridex.kafka.dlt-replay.timeout:30s}")
                Duration pollingTimeout
        ) {
                this.bootstrapServers = bootstrapServers;
                this.pollingTimeout = pollingTimeout;
        }

        @Override
        public void replay(String testId) throws Exception {

                Properties consumerProps = new Properties();
                consumerProps.put("bootstrap.servers", bootstrapServers);
                consumerProps.put("group.id", "ridex-dlt-replay-" + testId);
                consumerProps.put("auto.offset.reset", "earliest");
                consumerProps.put("enable.auto.commit", "false");
                consumerProps.put(
                                "key.deserializer",
                                StringDeserializer.class.getName());
                consumerProps.put(
                                "value.deserializer",
                                StringDeserializer.class.getName());

                ConsumerRecord<String, String> matchingRecord = null;
                long deadline = System.currentTimeMillis() + pollingTimeout.toMillis();

                try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {

                        consumer.subscribe(List.of(DLT_TOPIC));

                        while (System.currentTimeMillis() < deadline
                                        && matchingRecord == null) {

                                var records = consumer.poll(Duration.ofMillis(500));

                                for (ConsumerRecord<String, String> rec : records) {
                                        if (testId.equals(rec.key())) {
                                                matchingRecord = rec;
                                                break;
                                        }
                                }
                        }
                }

                if (matchingRecord == null) {
                        throw new IllegalStateException(
                                        "No DLT record found for test ID: " + testId);
                }

                Map<String, Object> producerProps = new HashMap<>();
                producerProps.put("bootstrap.servers", bootstrapServers);
                producerProps.put(
                                "key.serializer",
                                StringSerializer.class.getName());
                producerProps.put(
                                "value.serializer",
                                StringSerializer.class.getName());

                var producerFactory = new DefaultKafkaProducerFactory<String, String>(
                                producerProps);

                try {
                        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);

                        var result = template.send(
                                        new ProducerRecord<>(
                                                        TARGET_TOPIC,
                                                        matchingRecord.key(),
                                                        matchingRecord.value()))
                                        .get();

                        System.out.printf(
                                        "Replayed testId=%s to topic=%s partition=%d offset=%d%n",
                                        testId,
                                        TARGET_TOPIC,
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                } finally {
                        producerFactory.destroy();
                }
        }
}
