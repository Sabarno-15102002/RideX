package com.ridex.trip.config;

import com.ridex.trip.event.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class KafkaConsumerConfigTest {
    private KafkaConsumerConfig config;

    @BeforeEach void setUp() {
        config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
    }

    static Stream<Arguments> consumerFactories() {
        return Stream.of(
                Arguments.of("riderCreatedConsumerFactory", RiderCreatedEvent.class),
                Arguments.of("driverMatchRequestedConsumerFactory", DriverMatchRequestedEvent.class),
                Arguments.of("driverRideAcceptedConsumerFactory", DriverRideAcceptedEvent.class),
                Arguments.of("driverRideRejectedConsumerFactory", DriverRideRejectedEvent.class),
                Arguments.of("driverRideExpiredConsumerFactory", DriverRideExpiredEvent.class),
                Arguments.of("driverArrivedConsumerFactory", DriverArrivedEvent.class),
                Arguments.of("driverTripStartedConsumerFactory", DriverTripStartedEvent.class),
                Arguments.of("driverTripCompletedConsumerFactory", DriverTripCompletedEvent.class),
                Arguments.of("paymentSucceededConsumerFactory", PaymentSucceededEvent.class),
                Arguments.of("paymentFailedConsumerFactory", PaymentFailedEvent.class),
                Arguments.of("paymentCreatedConsumerFactory", PaymentCreatedEvent.class),
                Arguments.of("paymentRefundedConsumerFactory", PaymentRefundedEvent.class)
        );
    }

    @ParameterizedTest
    @MethodSource("consumerFactories")
    void consumerFactory_shouldUseCorrectEventTypeAndSafeBaseProperties(
            String factoryMethodName, Class<?> expectedEventType) throws Exception {
        Method method = KafkaConsumerConfig.class.getMethod(factoryMethodName);
        DefaultKafkaConsumerFactory<?, ?> factory =
                (DefaultKafkaConsumerFactory<?, ?>) method.invoke(config);
        Map<String, Object> properties = factory.getConfigurationProperties();

        assertEquals("localhost:9092", properties.get("bootstrap.servers"));
        assertEquals(expectedEventType, properties.get(JsonDeserializer.VALUE_DEFAULT_TYPE));
        assertEquals(ErrorHandlingDeserializer.class,
                properties.get("key.deserializer"));
        assertEquals(ErrorHandlingDeserializer.class,
                properties.get("value.deserializer"));
        assertEquals(false, properties.get("enable.auto.commit"));
        assertEquals("earliest", properties.get("auto.offset.reset"));
        assertEquals("com.ridex.trip.event", properties.get(JsonDeserializer.TRUSTED_PACKAGES));
    }
}
