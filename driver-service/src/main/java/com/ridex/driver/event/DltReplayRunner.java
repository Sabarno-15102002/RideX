package com.ridex.driver.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
@ConditionalOnProperty(
        name = "ridex.kafka.dlt-replay.enabled",
        havingValue = "true"
)
public class DltReplayRunner implements ApplicationRunner {

    private static final String DLT_TOPIC = "ridex.dlt.test.DLT";
    private static final String TARGET_TOPIC = "ridex.dlt.test";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String testId = args.getOptionValues("test-id") == null
                ? null
                : args.getOptionValues("test-id").get(0);

        if (testId == null || testId.isBlank()) {
            throw new IllegalArgumentException(
                    "Pass --test-id=<testId> to select the DLT record"
            );
        }

        String bootstrapServers = System.getenv().getOrDefault(
                "SPRING_KAFKA_BOOTSTRAP_SERVERS",
                "kafka:9092"
        );

        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", bootstrapServers);
        consumerProps.put("group.id", "ridex-dlt-replay-" + testId);
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("enable.auto.commit", "false");
        consumerProps.put(
                "key.deserializer",
                StringDeserializer.class.getName()
        );
        consumerProps.put(
                "value.deserializer",
                StringDeserializer.class.getName()
        );

        ConsumerRecord<String, String> matchingRecord = null;
        long deadline = System.currentTimeMillis() + 30_000;

        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(consumerProps)) {

            consumer.subscribe(List.of(DLT_TOPIC));

            while (System.currentTimeMillis() < deadline
                    && matchingRecord == null) {

                var records = consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {
                    if (testId.equals(record.key())) {
                        matchingRecord = record;
                        break;
                    }
                }
            }
        }

        if (matchingRecord == null) {
            throw new IllegalStateException(
                    "No DLT record found for test ID: " + testId
            );
        }

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put("bootstrap.servers", bootstrapServers);
        producerProps.put(
                "key.serializer",
                StringSerializer.class.getName()
        );
        producerProps.put(
                "value.serializer",
                StringSerializer.class.getName()
        );

        var producerFactory =
                new DefaultKafkaProducerFactory<String, String>(
                        producerProps
                );

        try {
            KafkaTemplate<String, String> template =
                    new KafkaTemplate<>(producerFactory);

            var result = template.send(
                    new ProducerRecord<>(
                            TARGET_TOPIC,
                            matchingRecord.key(),
                            matchingRecord.value()
                    )
            ).get();

            System.out.printf(
                    "Replayed testId=%s to topic=%s partition=%d offset=%d%n",
                    testId,
                    TARGET_TOPIC,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        } finally {
            producerFactory.destroy();
        }
    }
}