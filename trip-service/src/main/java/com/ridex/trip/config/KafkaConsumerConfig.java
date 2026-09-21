package com.ridex.trip.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.ridex.trip.event.event.DriverArrivedEvent;
import com.ridex.trip.event.event.DriverMatchRequestedEvent;
import com.ridex.trip.event.event.DriverRideAcceptedEvent;
import com.ridex.trip.event.event.DriverRideExpiredEvent;
import com.ridex.trip.event.event.DriverRideRejectedEvent;
import com.ridex.trip.event.event.DriverTripCompletedEvent;
import com.ridex.trip.event.event.DriverTripStartedEvent;
import com.ridex.trip.event.event.RiderCreatedEvent;

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
        public ConsumerFactory<String, RiderCreatedEvent> riderCreatedConsumerFactory() {

                Map<String, Object> config = consumerProperties();

                config.put(
                                JsonDeserializer.VALUE_DEFAULT_TYPE,
                                RiderCreatedEvent.class);

                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConsumerFactory<String, DriverMatchRequestedEvent> driverMatchRequestedConsumerFactory() {

                Map<String, Object> config = consumerProperties();

                config.put(
                                JsonDeserializer.VALUE_DEFAULT_TYPE,
                                DriverMatchRequestedEvent.class);

                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConsumerFactory<String, DriverRideAcceptedEvent> driverRideAcceptedConsumerFactory() {

                Map<String, Object> config = consumerProperties();

                config.put(
                                JsonDeserializer.VALUE_DEFAULT_TYPE,
                                DriverRideAcceptedEvent.class);

                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConsumerFactory<String, DriverRideRejectedEvent> driverRideRejectedConsumerFactory() {

                Map<String, Object> config = consumerProperties();

                config.put(
                                JsonDeserializer.VALUE_DEFAULT_TYPE,
                                DriverRideAcceptedEvent.class);

                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConsumerFactory<String, DriverRideExpiredEvent> driverRideExpiredConsumerFactory() {

                Map<String, Object> config = consumerProperties();

                config.put(
                                JsonDeserializer.VALUE_DEFAULT_TYPE,
                                DriverRideAcceptedEvent.class);

                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConsumerFactory<String, DriverArrivedEvent> driverArrivedConsumerFactory() {

                Map<String, Object> config = consumerProperties();

                config.put(
                                JsonDeserializer.VALUE_DEFAULT_TYPE,
                                DriverArrivedEvent.class);

                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConsumerFactory<String, DriverTripStartedEvent> driverTripStartedConsumerFactory() {

                Map<String, Object> config = consumerProperties();

                config.put(
                                JsonDeserializer.VALUE_DEFAULT_TYPE,
                                DriverTripStartedEvent.class);

                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConsumerFactory<String, DriverTripCompletedEvent> driverTripCompletedConsumerFactory() {

                Map<String, Object> config = consumerProperties();

                config.put(
                                JsonDeserializer.VALUE_DEFAULT_TYPE,
                                DriverTripCompletedEvent.class);

                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, RiderCreatedEvent> riderCreatedKafkaListenerContainerFactory(
                        ConsumerFactory<String, RiderCreatedEvent> consumerFactory) {

                ConcurrentKafkaListenerContainerFactory<String, RiderCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory);

                return factory;
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, DriverMatchRequestedEvent> driverMatchRequestedKafkaListenerContainerFactory(
                        ConsumerFactory<String, DriverMatchRequestedEvent> consumerFactory) {

                ConcurrentKafkaListenerContainerFactory<String, DriverMatchRequestedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory);

                return factory;
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, DriverRideAcceptedEvent> driverRideAcceptedKafkaListenerContainerFactory(
                        ConsumerFactory<String, DriverRideAcceptedEvent> consumerFactory) {

                ConcurrentKafkaListenerContainerFactory<String, DriverRideAcceptedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory);

                return factory;
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, DriverRideRejectedEvent> driverRideRejectedKafkaListenerContainerFactory(
                        ConsumerFactory<String, DriverRideRejectedEvent> consumerFactory) {

                ConcurrentKafkaListenerContainerFactory<String, DriverRideRejectedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory);

                return factory;
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, DriverRideExpiredEvent> driverRideExpiredKafkaListenerContainerFactory(
                        ConsumerFactory<String, DriverRideExpiredEvent> consumerFactory) {

                ConcurrentKafkaListenerContainerFactory<String, DriverRideExpiredEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory);

                return factory;
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, DriverArrivedEvent> driverArrivedKafkaListenerContainerFactory(
                        ConsumerFactory<String, DriverArrivedEvent> consumerFactory) {

                ConcurrentKafkaListenerContainerFactory<String, DriverArrivedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory);

                return factory;
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, DriverTripStartedEvent> driverTripStartedKafkaListenerContainerFactory(
                        ConsumerFactory<String, DriverTripStartedEvent> consumerFactory) {

                ConcurrentKafkaListenerContainerFactory<String, DriverTripStartedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory);

                return factory;
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, DriverTripCompletedEvent> driverTripCompletedKafkaListenerContainerFactory(
                        ConsumerFactory<String, DriverTripCompletedEvent> consumerFactory) {

                ConcurrentKafkaListenerContainerFactory<String, DriverTripCompletedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

                factory.setConsumerFactory(consumerFactory);

                return factory;
        }

}