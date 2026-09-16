package com.ridex.matching.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.ridex.matching.event.TripRematchingEvent;
import com.ridex.matching.event.TripRequestedEvent;

@Configuration
public class KafkaConsumerConfig {

        @Value("${spring.kafka.bootstrap-servers}")
        private String bootstrapServers;

        private Map<String, Object> consumerProperties() {

                Map<String, Object> config = new HashMap<>();

                config.put(
                                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                bootstrapServers);

                config.put(
                                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                ErrorHandlingDeserializer.class);

                config.put(
                                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                ErrorHandlingDeserializer.class);

                config.put(
                                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                                JsonDeserializer.class);

                config.put(
                                JsonDeserializer.TRUSTED_PACKAGES,
                                "com.ridex.trip.event");

                config.put(
                                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                                false);

                config.put(
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                                "earliest");

                return config;
        }

        @Bean
        public ConsumerFactory<String, TripRequestedEvent> tripRequestedConsumerFactory() {

                Map<String, Object> config = consumerProperties();

                config.put(
                                JsonDeserializer.VALUE_DEFAULT_TYPE,
                                TripRequestedEvent.class);

                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConsumerFactory<String, TripRematchingEvent> tripRematchingConsumerFactory() {

                Map<String, Object> config = consumerProperties();

                config.put(
                                JsonDeserializer.VALUE_DEFAULT_TYPE,
                                TripRematchingEvent.class);

                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, TripRequestedEvent> tripRequestedKafkaListenerContainerFactory(
                        ConsumerFactory<String, TripRequestedEvent> consumerFactory,
                        DefaultErrorHandler errorHandler) {

                ConcurrentKafkaListenerContainerFactory<String, TripRequestedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory);

                return factory;
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, TripRematchingEvent> tripRematchingKafkaListenerContainerFactory(
                        ConsumerFactory<String, TripRematchingEvent> consumerFactory,
                        DefaultErrorHandler errorHandler) {

                ConcurrentKafkaListenerContainerFactory<String, TripRematchingEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory);

                return factory;
        }
}